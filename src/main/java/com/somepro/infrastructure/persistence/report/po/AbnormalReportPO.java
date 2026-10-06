package com.somepro.infrastructure.persistence.report.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_abnormal_report 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」。表按现状用：没有 handled_at 列，处置时刻由 BasePO 的
 * update_time 审计列承担（每次推进走 UPDATE，MetaObjectHandler 自动刷新）。
 */
@Getter
@Setter
@TableName("t_abnormal_report")
public class AbnormalReportPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("report_no")
    private String reportNo;

    @TableField("obs_id")
    private Long obsId;

    @TableField("site_id")
    private Long siteId;

    @TableField("category")
    private String category;

    @TableField("severity")
    private String severity;

    @TableField("status")
    private String status;

    @TableField("reported_at")
    private LocalDateTime reportedAt;
}
