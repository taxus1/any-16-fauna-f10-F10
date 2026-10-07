package com.somepro.infrastructure.persistence.alert.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_epi_alert 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」：一条预警一行，立起来落在已发布，解除时刻在推进到已解除时记。
 * 表按现状用：没有处置时刻列，每推一步的时刻由 BasePO 的 update_time 审计列承担
 * （每次推进走 UPDATE，MetaObjectHandler 自动刷新）。
 */
@Getter
@Setter
@TableName("t_epi_alert")
public class EpiAlertPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("alert_no")
    private String alertNo;

    @TableField("report_id")
    private Long reportId;

    @TableField("sample_id")
    private Long sampleId;

    @TableField("alert_level")
    private String alertLevel;

    @TableField("status")
    private String status;

    @TableField("disposal_method")
    private String disposalMethod;

    @TableField("raised_at")
    private LocalDateTime raisedAt;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
}
