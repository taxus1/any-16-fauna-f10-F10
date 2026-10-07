package com.somepro.domain.site.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.site.model.MonitorSite;
import reactor.core.publisher.Mono;

/**
 * 监测点聚合的仓储端口：由领域层定义，基础设施层实现。
 */
public interface MonitorSiteRepository {

    /** 新建落库；siteNo 为空时由实现侧按 MP-YYYY-NNNN 生成，非空时按指定编号落库（撞号转业务异常）。 */
    Mono<MonitorSite> create(MonitorSite site);

    /** 按 id 更新资料（编号不改）。 */
    Mono<MonitorSite> update(MonitorSite site);

    Mono<MonitorSite> findById(Long id);

    /**
     * 按 id 查点位，含已撤点的（del_flag=1 也查得到）—— 事件线倒查专用：
     * 撤点是后来发生的，当初是在这个点上巡的。名单类查询仍走 {@link #findById}。
     */
    Mono<MonitorSite> findAnyById(Long id);

    /** 条件分页：条件全空时返回整份名册。 */
    Mono<PageResult<MonitorSite>> page(int pageNum, int pageSize,
                                       Long stationId, String siteType, String habitat, String status);

    /** 某站名下某状态的点位数量（监测站详情统计、关闭前校验共用这一份账）。 */
    Mono<Long> countByStationIdAndStatus(Long stationId, String status);

    /** 撤点：逻辑删除（@TableLogic 置 del_flag=1），账仍留在表里。 */
    Mono<Void> softDelete(Long id);
}
