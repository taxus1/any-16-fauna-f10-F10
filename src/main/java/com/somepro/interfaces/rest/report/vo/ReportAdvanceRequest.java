package com.somepro.interfaces.rest.report.vo;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 处置推进请求（VO，用户接口层）。
 *
 * targetStatus 四选一：HANDLING 处置中 / RESCUED 已救护 / SAMPLED 已采样 / CLOSED 已结案；
 * 只能顺着走（已上报→处置中→已救护/已采样→已结案），不能跳级、不能回退，结案后不再推。
 */
public record ReportAdvanceRequest(
        @NotBlank(message = "目标状态不能为空") String targetStatus) implements Serializable {
}
