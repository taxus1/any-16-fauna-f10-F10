package com.somepro.domain.sample.repository;

import com.somepro.domain.sample.model.SampleTest;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 采样送检与检测的仓储端口（领域层定义，基础设施层实现）。
 */
public interface SampleTestRepository {

    /**
     * 登记落库；sampleNo 由实现侧按 SM-YYYY-NNNN 生成，并发撞号自动重取，不甩底层冲突。
     */
    Mono<SampleTest> create(SampleTest sample);

    /**
     * 按 id 查看在册样本（del_flag=0）。
     */
    Mono<SampleTest> findById(Long id);

    /**
     * 倒一条上报下送的全部在册样本（一份上报可多次采样送检），按 id 升序，次序稳定。
     * 已删除（del_flag=1）的样本不出现（@TableLogic 自动过滤），没有时返回空列表。
     * 事件线倒查用：已销掉的样本不再串进链里；待检没出结果的也算发生过，照留。
     */
    Mono<List<SampleTest>> findActiveByReportId(Long reportId);

    /**
     * 检测结果落库 + 联动，一个事务三头一起动：
     * 样本按 result=PENDING 条件更新（同一条样本只翻得动一次，并发/重复录入在这被拦），
     * 上报从在办（已上报/处置中）推到已采样；上报已结案或已不走采样线的，整体回滚报错。
     * 结果一录成阳性，同一个事务里再立一条疫病预警（同一份阳性样本只落一条，已有就不再落）。
     */
    Mono<SampleTest> recordResult(SampleTest sample);

    /**
     * 条件分页：上报/样本类型/结果随意拼，全空翻整份在册样本，每行带样本编号。
     */
    Mono<PageResult<SampleTest>> page(int pageNum, int pageSize,
                                      Long reportId, String sampleType, String result);
}
