package com.somepro.interfaces.rest.timeline.vo;

import com.somepro.interfaces.rest.alert.vo.AlertVO;
import com.somepro.interfaces.rest.obs.vo.ObsVO;
import com.somepro.interfaces.rest.report.vo.ReportVO;
import com.somepro.interfaces.rest.sample.vo.SampleVO;
import com.somepro.interfaces.rest.site.vo.SiteVO;
import com.somepro.interfaces.rest.station.vo.StationVO;
import com.somepro.interfaces.rest.task.vo.PatrolTaskVO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件线段落对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * segmentType 标明这段是哪段（SITE/TASK/OBS/REPORT/SAMPLE/ALERT）；
 * 每种段只填自己那摊字段，其余槽位为空（JSON 里不出现，全局 non_null 序列化）：
 * 站点段带 station+site、任务段带 task、观测段带 obs、上报段带 report、
 * 样本段带 sample、预警段带 alert。各段的编号（stationNo/siteNo/taskNo/obsNo/
 * reportNo/sampleNo/alertNo）都在各自那摊里，好让人一段段对得上。
 *
 * occurredAt 是这段在线上的排序时刻（晚的顶在最前）：站点段取点位登记时刻、
 * 任务段取派发时刻、观测段取观测时刻、上报段取上报时刻、样本段取送检时刻、
 * 预警段取发布时刻。
 */
public record TimelineSegmentVO(String segmentType, LocalDateTime occurredAt,
                                StationVO station, SiteVO site,
                                PatrolTaskVO task, ObsVO obs,
                                ReportVO report, SampleVO sample,
                                AlertVO alert) implements Serializable {
}
