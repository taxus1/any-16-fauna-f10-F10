package com.somepro.domain.obs.model;

import java.util.Set;

/**
 * 一趟巡护的观测账（领域值对象，不可变 record）：任务名下观测记录的总条数与异常条数。
 *
 * 完成回报时按这个口径把账归拢写回任务（obs_count / abnormal_count），
 * 与观测录入读同一张 t_wildlife_obs、同一套 del_flag 过滤，两边数字才对得上。
 */
public record ObsSummary(int obsCount, int abnormalCount) {

    /** 健康状态：正常 */
    public static final String HEALTH_NORMAL = "NORMAL";
    /** 健康状态：受伤 */
    public static final String HEALTH_INJURED = "INJURED";
    /** 健康状态：死亡 */
    public static final String HEALTH_DEAD = "DEAD";
    /** 健康状态：疑似疫病 */
    public static final String HEALTH_SUSPECT = "SUSPECT";

    /** 异常口径：受伤 / 死亡 / 疑似疫病 —— 数异常条数就按这个集合。 */
    public static final Set<String> ABNORMAL_HEALTH = Set.of(HEALTH_INJURED, HEALTH_DEAD, HEALTH_SUSPECT);
}
