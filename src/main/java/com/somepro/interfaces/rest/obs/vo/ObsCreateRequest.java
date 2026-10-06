package com.somepro.interfaces.rest.obs.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 录入野生动物观测请求（VO，用户接口层）。
 *
 * 前置由应用层把关：任务必须正在执行、点位必须存在、物种必须在名录且启用。
 * healthStatus 可空（默认 NORMAL 正常）；observedAt 可空（取登记当下）；recorder 记录人可空。
 */
public record ObsCreateRequest(
        @NotNull(message = "巡护任务不能为空") Long taskId,
        @NotNull(message = "监测点不能为空") Long siteId,
        @NotBlank(message = "物种编码不能为空") String speciesCode,
        @NotNull(message = "个体数量不能为空")
        @Positive(message = "个体数量必须为正数，零和负数不收") Integer individualCount,
        String healthStatus,
        LocalDateTime observedAt,
        String recorder) implements Serializable {
}
