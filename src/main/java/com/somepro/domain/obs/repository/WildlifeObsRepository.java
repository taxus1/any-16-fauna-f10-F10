package com.somepro.domain.obs.repository;

import com.somepro.domain.obs.model.ObsSummary;
import reactor.core.publisher.Mono;

/**
 * 野生动物观测记录的仓储端口（领域层定义，基础设施层实现）。
 *
 * 当前只承载「按任务归拢观测账」这一读能力，供巡护任务完成回报时汇总；
 * 观测录入（写侧）与任务执行共用同一个端口、同一张表，两边数字才一致。
 */
public interface WildlifeObsRepository {

    /**
     * 归拢某任务名下的观测账：观测记录总条数，以及其中异常（受伤/死亡/疑似疫病）的条数。
     * 只数在册记录（del_flag=0），与观测录入的可见口径一致。
     */
    Mono<ObsSummary> summarizeByTaskId(Long taskId);
}
