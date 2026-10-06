package com.somepro.interfaces.rest.obs.vo;

import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改野生动物观测请求（VO，用户接口层）。
 *
 * 字段全部可空：传啥改啥，不传不动；任务归属（taskId）不开放修改。
 * 换点位要重新验点在册；换物种要重新验名录且启用，并照新物种当前级别重抄保护级别快照。
 */
public record ObsUpdateRequest(
        Long siteId,
        String speciesCode,
        @Positive(message = "个体数量必须为正数，零和负数不收") Integer individualCount,
        String healthStatus,
        LocalDateTime observedAt,
        String recorder) implements Serializable {
}
