package com.somepro.interfaces.rest.alert.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 疫病预警对外返回对象（VO，用户接口层）—— 不可变 record。预警编号 alertNo 必带，
 * 跟预警台账对得上号。
 *
 * handledAt 是最近处置时刻：表按现状用（无处置时刻列），取审计列 update_time ——
 * 每次处置推进走 UPDATE 时由 MetaObjectHandler 自动刷新；还没推进过的预警，它是发布时刻。
 */
public record AlertVO(Long id, String alertNo, Long reportId, Long sampleId, String alertLevel,
                      String status, String disposalMethod, LocalDateTime raisedAt,
                      LocalDateTime resolvedAt, LocalDateTime handledAt,
                      LocalDateTime createTime) implements Serializable {
}
