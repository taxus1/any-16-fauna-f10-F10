package com.somepro.interfaces.rest.report.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常个体上报对外返回对象（VO，用户接口层）—— 不可变 record。上报编号 reportNo 必带，
 * 跟纸质异常记录本对得上号。
 *
 * handledAt 是最近处置时刻：表按现状用（无 handled_at 列），取审计列 update_time ——
 * 每次处置推进走 UPDATE 时由 MetaObjectHandler 自动刷新；还没推进过的单，它是登记时刻。
 */
public record ReportVO(Long id, String reportNo, Long obsId, Long siteId, String category,
                       String severity, String status, LocalDateTime reportedAt,
                       LocalDateTime handledAt, LocalDateTime createTime) implements Serializable {
}
