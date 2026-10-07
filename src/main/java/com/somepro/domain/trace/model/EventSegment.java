package com.somepro.domain.trace.model;

/**
 * 观测事件线上各段的类型（领域读模型）。
 *
 * 同一条观测能倒出整条线：观测所属的监测站和监测点、挂的巡护任务、观测本身、
 * 由它发起的异常上报、上报下送的样本、样本触发的预警 —— 每一段一个类型。
 *
 * 常量上的序号（rank）只用于「同一时刻谁先谁后」的稳当兜底排序，不参与任何业务判断：
 * 按事情发生的先后倒着摆（最近顶最前），同一时刻的让链路上越靠后（越晚发起）的段排得越前。
 */
public final class EventSegment {

    /** 监测站 */
    public static final String STATION = "STATION";
    /** 监测点 */
    public static final String SITE = "SITE";
    /** 巡护任务 */
    public static final String TASK = "TASK";
    /** 野生动物观测 */
    public static final String OBS = "OBS";
    /** 异常个体上报 */
    public static final String REPORT = "REPORT";
    /** 采样送检与检测 */
    public static final String SAMPLE = "SAMPLE";
    /** 疫病预警 */
    public static final String ALERT = "ALERT";

    /**
     * 同刻兜底优先级：链路上越靠后的环节序号越大，倒序时越靠前。
     * 站 10 / 点 20 / 任务 30 / 观测 40 / 上报 50 / 样本 60 / 预警 70。
     * 未知段给 0，排最后，不影响既有次序。
     */
    public static int rankOf(String segment) {
        return switch (segment) {
            case STATION -> 10;
            case SITE -> 20;
            case TASK -> 30;
            case OBS -> 40;
            case REPORT -> 50;
            case SAMPLE -> 60;
            case ALERT -> 70;
            default -> 0;
        };
    }

    private EventSegment() {
    }
}
