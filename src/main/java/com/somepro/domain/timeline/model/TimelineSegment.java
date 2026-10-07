package com.somepro.domain.timeline.model;

import com.somepro.domain.alert.model.EpiAlert;
import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.domain.sample.model.SampleTest;
import com.somepro.domain.site.model.MonitorSite;
import com.somepro.domain.station.model.MonitorStation;
import com.somepro.domain.task.model.PatrolTask;

import java.time.LocalDateTime;

/**
 * 事件线上的一段（领域值对象）：是哪段（kind）、这段算在哪一刻（occurredAt）、
 * 段里装的是谁 —— 按 kind 占住对应槽位，其余槽位空（站点段占 station+site 两个槽）。
 *
 * occurredAt 取这段「事情发生」的那一刻：站点段取点位登记时刻、任务段取派发时刻、
 * 观测段取观测时刻、上报段取上报时刻、样本段取送检时刻、预警段取发布时刻；
 * 时刻列空了的拿登记时刻（create_time）兜底，再空就空着（排序时垫底）。
 */
public record TimelineSegment(SegmentKind kind, LocalDateTime occurredAt,
                              MonitorStation station, MonitorSite site,
                              PatrolTask task, WildlifeObs obs,
                              AbnormalReport report, SampleTest sample,
                              EpiAlert alert) {

    /** 站点段：站与点一段里两头都带；站没了（查不到）点位还在就照出点位。 */
    public static TimelineSegment site(MonitorStation station, MonitorSite site) {
        LocalDateTime at = site.getCreateTime() != null ? site.getCreateTime()
                : station != null ? station.getCreateTime() : null;
        return new TimelineSegment(SegmentKind.SITE, at, station, site, null, null, null, null, null);
    }

    /** 任务段：已取消（销账）的任务也照回 —— 取消是后来发生的，当初那趟巡护不能跟着没掉。 */
    public static TimelineSegment task(PatrolTask task) {
        return new TimelineSegment(SegmentKind.TASK, task.getCreateTime(),
                null, null, task, null, null, null, null);
    }

    public static TimelineSegment obs(WildlifeObs obs) {
        return new TimelineSegment(SegmentKind.OBS, firstNonNull(obs.getObservedAt(), obs.getCreateTime()),
                null, null, null, obs, null, null, null);
    }

    public static TimelineSegment report(AbnormalReport report) {
        return new TimelineSegment(SegmentKind.REPORT, firstNonNull(report.getReportedAt(), report.getCreateTime()),
                null, null, null, null, report, null, null);
    }

    public static TimelineSegment sample(SampleTest sample) {
        return new TimelineSegment(SegmentKind.SAMPLE, firstNonNull(sample.getSentAt(), sample.getCreateTime()),
                null, null, null, null, null, sample, null);
    }

    public static TimelineSegment alert(EpiAlert alert) {
        return new TimelineSegment(SegmentKind.ALERT, firstNonNull(alert.getRaisedAt(), alert.getCreateTime()),
                null, null, null, null, null, null, alert);
    }

    private static LocalDateTime firstNonNull(LocalDateTime moment, LocalDateTime fallback) {
        return moment != null ? moment : fallback;
    }
}
