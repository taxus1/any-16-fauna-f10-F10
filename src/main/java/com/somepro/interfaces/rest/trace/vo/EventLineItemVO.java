package com.somepro.interfaces.rest.trace.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 观测事件线上的一段（对外 VO，用户接口层）—— 不可变 record。
 *
 * - segment：段类型 STATION 监测站 / SITE 监测点 / TASK 巡护任务 / OBS 观测 /
 *   REPORT 异常上报 / SAMPLE 样本 / ALERT 预警；
 * - bizNo：该段自己的业务编号（站点编号/点位编号/任务编号/观测编号/上报编号/样本编号/预警编号），
 *   对账照这个号一段段对；
 * - title：给人看的名称/摘要；
 * - status：该段当下状态（没结案的、处置过的、解除过的都原样在）；
 * - eventTime：这一段那件事发生的时刻，列表已按它倒序。
 */
public record EventLineItemVO(String segment,
                              Long refId,
                              String bizNo,
                              String title,
                              String status,
                              LocalDateTime eventTime) implements Serializable {
}
