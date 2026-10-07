package com.somepro.interfaces.rest.alert.vo;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警处置推进请求（VO，用户接口层）。
 *
 * targetStatus 三选一：HANDLING 处置中 / RESOLVED 已解除 / CLOSED 已归档；
 * 只能顺着走（已发布→处置中→已解除→已归档），不能跳级、不能回退，归档后不再推。
 * disposalMethod 可空，带上就记下这一步的处置措施；resolvedAt 可空（取推进当下），
 * 仅推进到已解除时生效，解除时刻不能早于发布时刻。
 */
public record AlertAdvanceRequest(
        @NotBlank(message = "目标状态不能为空") String targetStatus,
        String disposalMethod,
        LocalDateTime resolvedAt) implements Serializable {
}
