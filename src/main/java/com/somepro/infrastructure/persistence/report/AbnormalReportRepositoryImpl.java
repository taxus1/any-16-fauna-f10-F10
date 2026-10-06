package com.somepro.infrastructure.persistence.report;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.domain.report.repository.AbnormalReportRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.obs.WildlifeObsMapper;
import com.somepro.infrastructure.persistence.report.converter.AbnormalReportPoConverter;
import com.somepro.infrastructure.persistence.report.po.AbnormalReportPO;
import com.somepro.infrastructure.persistence.support.BizNoGenerator;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 异常个体上报仓储适配器（基础设施层）。
 *
 * 编号分配：reportNo 按 AR-YYYY-NNNN 生成（年份按登记当下，序号 4 位零填充），
 * 并发撞号由 {@link BizNoGenerator} 重试，唯一索引兜底，一个号只落一条；
 * 作废记录占用的编号不复用（selectMaxSeq 的自定义 @Select 不拼 del_flag）。
 * 作废 = @TableLogic 逻辑删除（del_flag=1）：名单翻不到，账留在表里。
 *
 * 「同一观测只挂一条未作废上报」在登记事务内兜底：先 SELECT ... FOR UPDATE 锁住来源
 * 观测那一行，再数该观测名下未作废上报、落库 —— 两人前后脚一起递，后到者在行锁上排队，
 * 等前面那单提交后数到已有一条，报业务失败；原来那条作废了（del_flag=1 不计数）才放行重报。
 */
@Repository
public class AbnormalReportRepositoryImpl implements AbnormalReportRepository {

    /** 编号前缀：AR-（完整形如 AR-2026-） */
    private static final String NO_PREFIX = "AR-";

    private final AbnormalReportMapper reportMapper;
    private final WildlifeObsMapper obsMapper;
    private final TransactionTemplate transactionTemplate;

    public AbnormalReportRepositoryImpl(AbnormalReportMapper reportMapper,
                                        WildlifeObsMapper obsMapper,
                                        PlatformTransactionManager transactionManager) {
        this.reportMapper = reportMapper;
        this.obsMapper = obsMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<AbnormalReport> create(AbnormalReport report) {
        return blocking(() -> transactionTemplate.execute(txStatus -> {
            // 锁住来源观测那一行：同一观测的并发上报在这里排队，锁随事务提交/回滚释放
            obsMapper.lockById(report.getObsId());
            // 数在册上报（@TableLogic 自动拼 del_flag=0）：已有一条未作废的就不许再落
            Long active = reportMapper.selectCount(Wrappers.<AbnormalReportPO>lambdaQuery()
                    .eq(AbnormalReportPO::getObsId, report.getObsId()));
            if (active != null && active > 0) {
                throw new BizException("该观测已有一条未作废的上报，不能重复上报；原上报作废后才能重报");
            }
            String prefix = NO_PREFIX + LocalDate.now().getYear() + "-";
            return BizNoGenerator.insertWithRetry(
                    () -> reportMapper.selectMaxSeq(prefix, prefix.length() + 1),
                    prefix,
                    no -> doInsert(report, no));
        }));
    }

    @Override
    public Mono<AbnormalReport> findById(Long id) {
        return blocking(() -> {
            AbnormalReportPO po = reportMapper.selectById(id);
            return po == null ? null : AbnormalReportPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Boolean> advance(AbnormalReport report, String fromStatus) {
        return blocking(() -> {
            // 条件更新：只有仍处原状态的那一行才翻得动（并发推同一单只放行一下）；
            // SET 只带新状态，处置时刻由审计列 update_time 自动记下，del_flag=0 由 @TableLogic 拼上。
            AbnormalReportPO po = new AbnormalReportPO();
            po.setStatus(report.getStatus());
            int rows = reportMapper.update(po, Wrappers.<AbnormalReportPO>lambdaUpdate()
                    .eq(AbnormalReportPO::getId, report.getId())
                    .eq(AbnormalReportPO::getStatus, fromStatus));
            return rows == 1;
        });
    }

    @Override
    public Mono<Void> voidReport(Long id) {
        return blocking(() -> {
            // @TableLogic 把 deleteById 改写成 UPDATE t_abnormal_report SET del_flag=1
            // WHERE id=? AND del_flag=0：已作废的重复作废不动第二下，账仍留在表里。
            reportMapper.deleteById(id);
            return Boolean.TRUE;
        }).then();
    }

    @Override
    public Mono<PageResult<AbnormalReport>> page(int pageNum, int pageSize,
                                                 Long siteId, String category,
                                                 String severity, String status) {
        return this.<PageResult<AbnormalReport>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<AbnormalReportPO> wrapper = Wrappers.<AbnormalReportPO>lambdaQuery()
                        .eq(siteId != null, AbnormalReportPO::getSiteId, siteId)
                        .eq(hasText(category), AbnormalReportPO::getCategory, category)
                        .eq(hasText(severity), AbnormalReportPO::getSeverity, severity)
                        .eq(hasText(status), AbnormalReportPO::getStatus, status)
                        .orderByAsc(AbnormalReportPO::getId);
                List<AbnormalReportPO> rows = reportMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<AbnormalReport> content = rows.stream()
                        .map(AbnormalReportPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    private AbnormalReport doInsert(AbnormalReport report, String reportNo) {
        report.setReportNo(reportNo);
        AbnormalReportPO po = AbnormalReportPoConverter.toPo(report);
        po.setId(IdUtil.getSnowflakeNextId());
        reportMapper.insert(po);
        return AbnormalReportPoConverter.toDomain(po);
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
