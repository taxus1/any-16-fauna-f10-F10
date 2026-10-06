package com.somepro.infrastructure.persistence.task.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * t_patrol_task 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」，业务规则在领域对象 PatrolTask。
 * obs_count / abnormal_count / started_at / finished_at 由执行环节写入：
 * 开工写 started_at，完成回报一笔写 finished_at 与观测账；派发/修改/取消不碰它们，落库走表默认值。
 */
@Getter
@Setter
@TableName("t_patrol_task")
public class PatrolTaskPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("task_no")
    private String taskNo;

    @TableField("station_id")
    private Long stationId;

    @TableField("site_id")
    private Long siteId;

    @TableField("patrol_type")
    private String patrolType;

    @TableField("planned_date")
    private LocalDate plannedDate;

    @TableField("executor")
    private String executor;

    @TableField("status")
    private String status;

    @TableField("obs_count")
    private Integer obsCount;

    @TableField("abnormal_count")
    private Integer abnormalCount;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
