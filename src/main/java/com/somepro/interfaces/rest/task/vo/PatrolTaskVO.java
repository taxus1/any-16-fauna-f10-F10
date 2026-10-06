package com.somepro.interfaces.rest.task.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡护任务对外返回对象（VO，用户接口层）—— 不可变 record。任务编号 taskNo 必带。
 *
 * 进度字段（status / startedAt / finishedAt / obsCount / abnormalCount）随任务推进逐步有值：
 * 待执行时开工/完成时刻与观测账还是空（JSON 里不出现），完成回报后账已归拢冻结。
 */
public record PatrolTaskVO(Long id, String taskNo, Long stationId, Long siteId, String patrolType,
                           LocalDate plannedDate, String executor, String status,
                           Integer obsCount, Integer abnormalCount,
                           LocalDateTime startedAt, LocalDateTime finishedAt,
                           LocalDateTime createTime) implements Serializable {
}
