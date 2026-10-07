package com.somepro.domain.alert.repository;

import com.somepro.domain.alert.model.EpiAlert;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 疫病预警与处置的仓储端口（领域层定义，基础设施层实现）。
 *
 * 预警的「立」不在这一侧：样本检测那条链上结果一录成阳性，预警就在同一个事务里
 * 跟着落库（见样本仓储适配器），这里只管立起来以后的处置推进、查看与翻页。
 */
public interface EpiAlertRepository {

    /**
     * 按 id 查看在册预警（del_flag=0）。
     */
    Mono<EpiAlert> findById(Long id);

    /**
     * 倒一条上报链上立起来的全部在册预警，按 id 升序，次序稳定。
     * 已删除（del_flag=1）的预警不出现（@TableLogic 自动过滤），没有时返回空列表。
     * 事件线倒查用：已销掉的预警不再串进链里；处置中/已解除/已归档的都留着，不因为已结案就抹掉。
     */
    Mono<List<EpiAlert>> findActiveByReportId(Long reportId);

    /**
     * 处置推进落库：按原状态条件更新（WHERE id=? AND status=fromStatus），状态只翻动一次。
     * 并发推同一条只有一下翻得动，另一下返回 false；每推一步的时刻由审计列 update_time 记下，
     * 推进到已解除时另记 resolved_at。
     * 推进到已解除时，同一个事务里把挂的那条上报跟着推到已结案；已经结案的照旧。
     *
     * @param fromStatus 推进前的原状态（应用层加载时读到的那个）
     * @return true 翻动成功；false 预警已不在原状态（被并发翻动）
     */
    Mono<Boolean> advance(EpiAlert alert, String fromStatus);

    /**
     * 条件分页：上报/样本/级别/状态随意拼，全空翻整份在册预警，每行带预警编号。
     */
    Mono<PageResult<EpiAlert>> page(int pageNum, int pageSize,
                                    Long reportId, Long sampleId, String alertLevel, String status);
}
