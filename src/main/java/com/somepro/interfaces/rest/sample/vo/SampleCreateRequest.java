package com.somepro.interfaces.rest.sample.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登记采样请求（VO，用户接口层）。
 *
 * 前置由应用层把关：上报得在册且还在办（已上报/处置中），已结案的别再采样。
 * 结果不用前端填，新登记的一律待检；sentAt 可空（取登记当下）。
 */
public record SampleCreateRequest(
        @NotNull(message = "所属上报不能为空") Long reportId,
        @NotBlank(message = "样本类型不能为空") String sampleType,
        LocalDateTime sentAt,
        String labName,
        String testItem) implements Serializable {
}
