package com.somepro.infrastructure.persistence.obs;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.domain.obs.model.ObsSummary;
import com.somepro.domain.obs.repository.WildlifeObsRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.obs.po.WildlifeObsPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 野生动物观测记录仓储适配器（基础设施层）。
 *
 * 数账口径：selectCount 走 @TableLogic 自动拼 del_flag=0，与观测录入可见的记录同一套过滤；
 * 异常按 {@link ObsSummary#ABNORMAL_HEALTH}（受伤/死亡/疑似疫病）数。
 */
@Repository
public class WildlifeObsRepositoryImpl implements WildlifeObsRepository {

    private final WildlifeObsMapper obsMapper;

    public WildlifeObsRepositoryImpl(WildlifeObsMapper obsMapper) {
        this.obsMapper = obsMapper;
    }

    @Override
    public Mono<ObsSummary> summarizeByTaskId(Long taskId) {
        return blocking(() -> {
            long obsCount = obsMapper.selectCount(Wrappers.<WildlifeObsPO>lambdaQuery()
                    .eq(WildlifeObsPO::getTaskId, taskId));
            long abnormalCount = obsMapper.selectCount(Wrappers.<WildlifeObsPO>lambdaQuery()
                    .eq(WildlifeObsPO::getTaskId, taskId)
                    .in(WildlifeObsPO::getHealthStatus, ObsSummary.ABNORMAL_HEALTH));
            return new ObsSummary(Math.toIntExact(obsCount), Math.toIntExact(abnormalCount));
        });
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
