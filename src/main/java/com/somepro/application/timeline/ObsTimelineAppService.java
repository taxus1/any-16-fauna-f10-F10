package com.somepro.application.timeline;

import com.somepro.domain.alert.model.EpiAlert;
import com.somepro.domain.alert.repository.EpiAlertRepository;
import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.obs.repository.WildlifeObsRepository;
import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.domain.report.repository.AbnormalReportRepository;
import com.somepro.domain.sample.model.SampleTest;
import com.somepro.domain.sample.repository.SampleTestRepository;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.site.model.MonitorSite;
import com.somepro.domain.site.repository.MonitorSiteRepository;
import com.somepro.domain.station.model.MonitorStation;
import com.somepro.domain.station.repository.MonitorStationRepository;
import com.somepro.domain.task.model.PatrolTask;
import com.somepro.domain.task.repository.PatrolTaskRepository;
import com.somepro.domain.timeline.model.ObsTimeline;
import com.somepro.domain.timeline.model.TimelineSegment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 观测事件线应用层：输入一个观测编号，把这条观测从头到尾的整条线倒出来 ——
 * 从哪个站哪个点巡的、什么时候看见的、报了没有、送检没有、最后立没立预警，
 * 一段都不能缺，供上级核账或异常复盘一段段对号。
 *
 * 串线口径：
 * - 观测编号查不到、或观测自己已销掉（作废）的，回空线，不报错；
 * - 站点段（站+点）与任务段照回当时的事实：站已关闭、点已撤、任务已取消（销账）的
 *   也照常回 —— 那些都是后来发生的，当初那趟巡护不能跟着没掉；
 * - 上报/样本/预警按各自的销账状态定进不进线：销掉（del_flag=1）的不串进来；
 *   没结案的、处置过的、解除过的都在册，都得留着，别因为结果还没出就当没发生；
 * - 销掉的上报/样本本身不进线，但它名下送过的样本、立过的预警是实打实发生过的，
 *   顺着它照样往下找（仓储层把含销账的整条链列出来，进不进线在这里按 delFlag 定）；
 * - 整条线按事情发生的先后倒着摆，最近的顶在最前；同一时刻的谁先谁后见
 *   {@link ObsTimeline} 的稳当次序。
 */
@Service
public class ObsTimelineAppService {

    private final WildlifeObsRepository obsRepository;
    private final PatrolTaskRepository taskRepository;
    private final MonitorSiteRepository siteRepository;
    private final MonitorStationRepository stationRepository;
    private final AbnormalReportRepository reportRepository;
    private final SampleTestRepository sampleRepository;
    private final EpiAlertRepository alertRepository;

    public ObsTimelineAppService(WildlifeObsRepository obsRepository,
                                 PatrolTaskRepository taskRepository,
                                 MonitorSiteRepository siteRepository,
                                 MonitorStationRepository stationRepository,
                                 AbnormalReportRepository reportRepository,
                                 SampleTestRepository sampleRepository,
                                 EpiAlertRepository alertRepository) {
        this.obsRepository = obsRepository;
        this.taskRepository = taskRepository;
        this.siteRepository = siteRepository;
        this.stationRepository = stationRepository;
        this.reportRepository = reportRepository;
        this.sampleRepository = sampleRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * 按观测编号倒整条事件线。编号空白、查不到、或观测已销掉的，都回空线（segments 为空），
     * 不报错 —— 核账的人拿个号来问，有没有这条线都给他一个准话。
     */
    public Mono<ObsTimeline> timeline(String obsNo) {
        String no = normalize(obsNo);
        if (no == null) {
            return Mono.just(ObsTimeline.empty(obsNo));
        }
        return obsRepository.findByObsNo(no)
                .flatMap(obs -> assemble(no, obs))
                .defaultIfEmpty(ObsTimeline.empty(no));
    }

    /** 归拢上游三段：站点（站+点）、任务、观测。任务/点位查「含销账」的，站/点/任务缺了不挡路。 */
    private Mono<ObsTimeline> assemble(String obsNo, WildlifeObs obs) {
        Mono<Optional<PatrolTask>> taskMono = taskRepository.findAnyById(obs.getTaskId())
                .map(Optional::of).defaultIfEmpty(Optional.empty());
        Mono<Optional<MonitorSite>> siteMono = siteRepository.findAnyById(obs.getSiteId())
                .map(Optional::of).defaultIfEmpty(Optional.empty());
        return Mono.zip(taskMono, siteMono)
                .flatMap(pair -> {
                    MonitorSite site = pair.getT2().orElse(null);
                    Mono<Optional<MonitorStation>> stationMono = site == null
                            ? Mono.just(Optional.<MonitorStation>empty())
                            : stationRepository.findById(site.getStationId())
                                    .map(Optional::of).defaultIfEmpty(Optional.empty());
                    return stationMono.flatMap(station -> assembleChain(obsNo, obs,
                            pair.getT1().orElse(null), site, station.orElse(null)));
                });
    }

    /** 顺着观测往下串：上报 → 样本 → 预警，一层层的 id 递下去，各层把整条链（含销账）列出来。 */
    private Mono<ObsTimeline> assembleChain(String obsNo, WildlifeObs obs, PatrolTask task,
                                            MonitorSite site, MonitorStation station) {
        return reportRepository.listAnyByObsId(obs.getId())
                .flatMap(reports -> {
                    List<Long> reportIds = reports.stream().map(AbnormalReport::getId).toList();
                    Mono<List<SampleTest>> samplesMono = reportIds.isEmpty()
                            ? Mono.just(List.<SampleTest>of())
                            : sampleRepository.listAnyByReportIds(reportIds);
                    return samplesMono.flatMap(samples -> {
                        List<Long> sampleIds = samples.stream().map(SampleTest::getId).toList();
                        Mono<List<EpiAlert>> alertsMono = sampleIds.isEmpty()
                                ? Mono.just(List.<EpiAlert>of())
                                : alertRepository.listBySampleIds(sampleIds);
                        return alertsMono.map(alerts -> ObsTimeline.of(obsNo,
                                buildSegments(obs, task, site, station, reports, samples, alerts)));
                    });
                });
    }

    /**
     * 拼段落：站点、任务、观测三段照当时的事实回；上报/样本/预警只串自己没销掉的
     * （销掉的不进线，没结案/处置过/解除过的都留着）。预警仓储只回在册的，不用再滤。
     * 段落按站点→任务→观测→上报→样本→预警、同种按 id 升序入列，排序交给
     * {@link ObsTimeline#of}（稳定排序，同刻同种的保持这个来样次序）。
     */
    private List<TimelineSegment> buildSegments(WildlifeObs obs, PatrolTask task,
                                                MonitorSite site, MonitorStation station,
                                                List<AbnormalReport> reports,
                                                List<SampleTest> samples,
                                                List<EpiAlert> alerts) {
        List<TimelineSegment> segments = new ArrayList<>();
        if (site != null) {
            segments.add(TimelineSegment.site(station, site));
        }
        if (task != null) {
            segments.add(TimelineSegment.task(task));
        }
        segments.add(TimelineSegment.obs(obs));
        reports.stream().filter(ObsTimelineAppService::alive)
                .forEach(report -> segments.add(TimelineSegment.report(report)));
        samples.stream().filter(ObsTimelineAppService::alive)
                .forEach(sample -> segments.add(TimelineSegment.sample(sample)));
        alerts.forEach(alert -> segments.add(TimelineSegment.alert(alert)));
        return segments;
    }

    /** 在册才算数：delFlag=0（或空，老数据兜底）的留下，销掉（1）的不串进线。 */
    private static boolean alive(BaseEntity entity) {
        return entity.getDelFlag() == null || entity.getDelFlag() == 0;
    }

    private static String normalize(String obsNo) {
        return (obsNo == null || obsNo.isBlank()) ? null : obsNo.trim();
    }
}
