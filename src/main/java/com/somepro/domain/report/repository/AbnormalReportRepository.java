package com.somepro.domain.report.repository;

import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 异常个体上报的仓储端口（领域层定义，基础设施层实现）。
 */
public interface AbnormalReportRepository {

    /**
     * 登记落库；reportNo 由实现侧按 AR-YYYY-NNNN 生成，并发撞号自动重取，不甩底层冲突。
     * 「同一观测只挂一条未作废上报」在实现侧事务内兜底：两人前后脚一起递也只落一条，
     * 后到者抛业务异常；原来那条作废了，再重报才放行。
     */
    Mono<AbnormalReport> create(AbnormalReport report);

    /**
     * 按 id 查看在册上报（del_flag=0）。已作废的翻不到，返回空。
     */
    Mono<AbnormalReport> findById(Long id);

    /**
     * 处置推进落库：按原状态条件更新（WHERE id=? AND status=fromStatus），状态只翻动一次。
     * 并发推同一单只有一下翻得动，另一下返回 false；处置时刻由审计列 update_time 记下。
     *
     * @param fromStatus 推进前的原状态（应用层加载时读到的那个）
     * @return true 翻动成功；false 上报已不在原状态（被并发翻动）
     */
    Mono<Boolean> advance(AbnormalReport report, String fromStatus);

    /**
     * 作废：逻辑删除（del_flag=1），名单里不再翻到，账仍留在表里。
     */
    Mono<Void> voidReport(Long id);

    /**
     * 条件分页：点位/类别/严重程度/状态随意拼，全空翻整份在册上报。
     * 已作废（del_flag=1）的不出现（@TableLogic 自动过滤），每行带上报编号。
     */
    Mono<PageResult<AbnormalReport>> page(int pageNum, int pageSize,
                                          Long siteId, String category, String severity, String status);
}
