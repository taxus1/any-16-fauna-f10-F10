package com.somepro.domain.obs.repository;

import com.somepro.domain.obs.model.ObsSummary;
import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 野生动物观测记录的仓储端口（领域层定义，基础设施层实现）。
 *
 * 读侧「按任务归拢观测账」供巡护任务完成回报时汇总；写侧（录入/修改/查看/作废/条件分页）
 * 由观测模块使用，两边共用同一张 t_wildlife_obs、同一套 del_flag 过滤，数字才对得上。
 */
public interface WildlifeObsRepository {

    /**
     * 录观测落库；obsNo 由实现侧按 WO-YYYY-NNNNNN 生成，并发撞号自动重取，不甩底层冲突。
     */
    Mono<WildlifeObs> create(WildlifeObs obs);

    /** 按 id 修改观测（编号、任务归属不改）。 */
    Mono<WildlifeObs> update(WildlifeObs obs);

    /**
     * 按 id 查看在册观测（del_flag=0）。已作废的翻不到，返回空。
     */
    Mono<WildlifeObs> findById(Long id);

    /**
     * 按观测编号查看在册观测（del_flag=0）—— 事件线倒查的入口。
     * 编号查不到、或观测自己已作废（销掉）的，都返回空，调用方照「查不到」回空线，不报错。
     */
    Mono<WildlifeObs> findByObsNo(String obsNo);

    /**
     * 作废：逻辑删除（del_flag=1），清单里不再翻到，底子仍留在库里备查。
     */
    Mono<Void> voidObs(Long id);

    /**
     * 条件分页：任务/点位/物种/健康状态随意拼，观测时刻区间可带上，全空翻整份在册观测。
     * 已作废（del_flag=1）的不出现（@TableLogic 自动过滤），每行带观测编号。
     */
    Mono<PageResult<WildlifeObs>> page(int pageNum, int pageSize,
                                       Long taskId, Long siteId, String speciesCode,
                                       String healthStatus,
                                       LocalDateTime observedFrom, LocalDateTime observedTo);

    /**
     * 归拢某任务名下的观测账：观测记录总条数，以及其中异常（受伤/死亡/疑似疫病）的条数。
     * 只数在册记录（del_flag=0），与观测录入的可见口径一致。
     */
    Mono<ObsSummary> summarizeByTaskId(Long taskId);
}
