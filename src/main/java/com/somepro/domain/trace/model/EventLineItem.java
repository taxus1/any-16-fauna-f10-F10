package com.somepro.domain.trace.model;

import java.time.LocalDateTime;

/**
 * 观测事件线上的一段（领域读模型，只读）。
 *
 * 一段对应一件事：站、点、任务、观测、上报、样本、预警各出各的段；
 * 每段都把自己的业务编号带出来（站点编号/点位编号/任务编号/观测编号/上报编号/样本编号/预警编号），
 * 好让对账的人拿着号一段段对得上。
 *
 * 各字段都是「这一段自己的」快照：
 * - segment：段类型，取 {@link EventSegment} 里的常量；
 * - bizNo：该段的业务编号（如 ST-2026-0001）；
 * - title：名称/摘要，给人看的一句（站名/点名/观测物种与数量/上报类别…）；
 * - status：该段当下状态（运行/在册/任务状态/健康状态/上报状态/检测结果/预警状态），
 *   没结案的、处置过的、解除过的都原样带出，不因为结果还没出就当没发生；
 * - eventTime：这一段那件事发生的时刻，整条线照它倒序；
 * - refId：该段行的雪花 id，对账定位用，同刻再按它定稳当次序。
 *
 * 该模型不做任何写操作，也不持有其他聚合，只是把各张表读出来的账摊平成一条线上的段。
 */
public record EventLineItem(String segment,
                            Long refId,
                            String bizNo,
                            String title,
                            String status,
                            LocalDateTime eventTime) {

    public static EventLineItem station(Long id, String stationNo, String name,
                                        String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.STATION, id, stationNo, name, status, eventTime);
    }

    public static EventLineItem site(Long id, String siteNo, String title,
                                     String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.SITE, id, siteNo, title, status, eventTime);
    }

    public static EventLineItem task(Long id, String taskNo, String title,
                                     String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.TASK, id, taskNo, title, status, eventTime);
    }

    public static EventLineItem obs(Long id, String obsNo, String title,
                                    String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.OBS, id, obsNo, title, status, eventTime);
    }

    public static EventLineItem report(Long id, String reportNo, String title,
                                       String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.REPORT, id, reportNo, title, status, eventTime);
    }

    public static EventLineItem sample(Long id, String sampleNo, String title,
                                       String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.SAMPLE, id, sampleNo, title, status, eventTime);
    }

    public static EventLineItem alert(Long id, String alertNo, String title,
                                      String status, LocalDateTime eventTime) {
        return new EventLineItem(EventSegment.ALERT, id, alertNo, title, status, eventTime);
    }
}
