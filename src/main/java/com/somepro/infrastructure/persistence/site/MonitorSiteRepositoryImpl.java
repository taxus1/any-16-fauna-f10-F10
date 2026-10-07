package com.somepro.infrastructure.persistence.site;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.site.model.MonitorSite;
import com.somepro.domain.site.repository.MonitorSiteRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.site.converter.MonitorSitePoConverter;
import com.somepro.infrastructure.persistence.site.po.MonitorSitePO;
import com.somepro.infrastructure.persistence.support.BizNoGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 监测点仓储适配器（基础设施层）。
 *
 * 编号分配：siteNo 为空时按 MP-YYYY-NNNN 生成（并发撞号由 {@link BizNoGenerator} 重试）；
 * 指定编号时撞唯一索引转业务异常。点位统计（countByStationIdAndStatus）与点位名单
 * 走同一张表、同一套 del_flag 过滤，监测站详情里的数字和点位模块自然对得上。
 */
@Repository
public class MonitorSiteRepositoryImpl implements MonitorSiteRepository {

    /** 编号前缀：MP-年-（如 MP-2026-） */
    private static final String NO_PREFIX = "MP-";

    private final MonitorSiteMapper siteMapper;

    public MonitorSiteRepositoryImpl(MonitorSiteMapper siteMapper) {
        this.siteMapper = siteMapper;
    }

    @Override
    public Mono<MonitorSite> create(MonitorSite site) {
        return blocking(() -> {
            if (site.getSiteNo() != null && !site.getSiteNo().isBlank()) {
                try {
                    return doInsert(site, site.getSiteNo().trim());
                } catch (DuplicateKeyException e) {
                    throw new BizException("监测点编号已存在：" + site.getSiteNo());
                }
            }
            String prefix = NO_PREFIX + LocalDate.now().getYear() + "-";
            return BizNoGenerator.insertWithRetry(
                    () -> siteMapper.selectMaxSeq(prefix, prefix.length() + 1),
                    prefix,
                    no -> doInsert(site, no));
        });
    }

    @Override
    public Mono<MonitorSite> update(MonitorSite site) {
        return blocking(() -> {
            MonitorSitePO po = MonitorSitePoConverter.toPo(site);
            siteMapper.updateById(po);
            return MonitorSitePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<MonitorSite> findById(Long id) {
        return blocking(() -> {
            MonitorSitePO po = siteMapper.selectById(id);
            return po == null ? null : MonitorSitePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<MonitorSite> findAnyById(Long id) {
        return blocking(() -> {
            // 自定义 @Select 不拼 del_flag：已撤的点位也查得到（事件线倒查用）。
            MonitorSitePO po = siteMapper.selectAnyById(id);
            return po == null ? null : MonitorSitePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<MonitorSite>> page(int pageNum, int pageSize,
                                              Long stationId, String siteType, String habitat, String status) {
        return this.<PageResult<MonitorSite>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<MonitorSitePO> wrapper = Wrappers.<MonitorSitePO>lambdaQuery()
                        .eq(stationId != null, MonitorSitePO::getStationId, stationId)
                        .eq(hasText(siteType), MonitorSitePO::getSiteType, siteType)
                        .eq(hasText(habitat), MonitorSitePO::getHabitat, habitat)
                        .eq(hasText(status), MonitorSitePO::getStatus, status)
                        .orderByAsc(MonitorSitePO::getId);
                List<MonitorSitePO> rows = siteMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<MonitorSite> content = rows.stream()
                        .map(MonitorSitePoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Long> countByStationIdAndStatus(Long stationId, String status) {
        return blocking(() -> siteMapper.selectCount(Wrappers.<MonitorSitePO>lambdaQuery()
                .eq(MonitorSitePO::getStationId, stationId)
                .eq(MonitorSitePO::getStatus, status)));
    }

    @Override
    public Mono<Void> softDelete(Long id) {
        return blocking(() -> {
            // @TableLogic：UPDATE t_monitor_site SET del_flag = 1 WHERE id = ? AND del_flag = 0
            siteMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    private MonitorSite doInsert(MonitorSite site, String siteNo) {
        site.setSiteNo(siteNo);
        MonitorSitePO po = MonitorSitePoConverter.toPo(site);
        po.setId(IdUtil.getSnowflakeNextId());
        siteMapper.insert(po);
        return MonitorSitePoConverter.toDomain(po);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 阻塞 DB 调用 → 响应式链路的桥接器：先取 Reactor Context 里的操作人，
     * 再切到 boundedElastic 执行 JDBC，操作人放进 AuditContextHolder 供审计填充。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
