package com.somepro.interfaces.rest.report.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登记异常上报请求（VO，用户接口层）。
 *
 * 前置由应用层把关：观测在册且健康状态不是正常、挂的巡护任务未取消、
 * 同一观测只挂一条未作废上报；类别得跟观测健康状态对得上。
 * 严重程度不用前端填，由系统按来头算；reportedAt 可空（取登记当下）。
 */
public record ReportCreateRequest(
        @NotNull(message = "来源观测不能为空") Long obsId,
        @NotBlank(message = "上报类别不能为空") String category,
        LocalDateTime reportedAt) implements Serializable {
}
