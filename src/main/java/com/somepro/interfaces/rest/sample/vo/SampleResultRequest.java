package com.somepro.interfaces.rest.sample.vo;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检测结果回填请求（VO，用户接口层）。
 *
 * 结果只录一次：还悬着（待检）的才录得了，同一条样本别来回翻；
 * 录进去的同时上报从在办推到已采样，两头一起动。testedAt 可空（取录入当下）。
 */
public record SampleResultRequest(
        @NotBlank(message = "检测结果不能为空") String result,
        LocalDateTime testedAt) implements Serializable {
}
