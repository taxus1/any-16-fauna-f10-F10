package com.somepro.domain.timeline.model;

/**
 * 事件线段落种类（领域层）。
 *
 * 声明顺序即「同一时刻」的稳当次序：站点 → 任务 → 观测 → 上报 → 样本 → 预警，
 * 靠上游的环节排前（{@link #ordinal()} 直接当 tie-break 用）。
 */
public enum SegmentKind {

    /** 站点段：观测所属的监测站和监测点（一段里两头都带） */
    SITE,

    /** 任务段：这条观测挂在的巡护任务 */
    TASK,

    /** 观测段：观测本身 */
    OBS,

    /** 上报段：由这条观测发起的异常上报 */
    REPORT,

    /** 样本段：上报下送的样本 */
    SAMPLE,

    /** 预警段：样本触发的预警 */
    ALERT
}
