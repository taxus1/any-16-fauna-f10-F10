package com.somepro.application.trace;

import com.somepro.domain.alert.model.EpiAlert;
import com.somepro.domain.alert.repository.EpiAlertRepository;
import com.somepro.domain.obs.model.ObsSummary;
import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.obs.repository.WildlifeObsRepository;
import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.domain.report.repository.AbnormalReportRepository;
import com.somepro.domain.sample.model.SampleTest;
import com.somepro.domain.sample.repository.SampleTestRepository;
import com.somepro.domain.site.model.MonitorSite;
import com.somepro.domain.site.repository.MonitorSiteRepository;
import com.somepro.domain.station.model.MonitorStation;
import com.somepro.domain.station.repository.MonitorStationRepository;
import com.somepro.domain.task.model.PatrolTask;
import com.somepro.domain.task.repository.PatrolTaskRepository;
import com.somepro.domain.trace.model.EventLineItem;
import com.somepro.domain.trace.model.EventSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ObsEventLineAppService} 单测：仓储全 Mock，不起 Spring、不碰库（返回的都是冷 Mono，直接 block）。
 * 钉住事件线的几条硬口径：查无/已作废回空、各段编号齐、倒序、同刻稳当次序、
 * 已销账段不串、待检/处置过的段照留。
 */
class ObsEventLineAppServiceTest {

    private WildlifeObsRepository obsRepository;
    private PatrolTaskRepository taskRepository;
    private MonitorSiteRepository siteRepository;
    private MonitorStationRepository stationRepository;
    private AbnormalReportRepository reportRepository;
    private SampleTestRepository sampleRepository;
    private EpiAlertRepository alertRepository;

    private ObsEventLineAppService service;

    @BeforeEach
    void setUp() {
        obsRepository = mock(WildlifeObsRepository.class);
        taskRepository = mock(PatrolTaskRepository.class);
        siteRepository = mock(MonitorSiteRepository.class);
        stationRepository = mock(MonitorStationRepository.class);
        reportRepository = mock(AbnormalReportRepository.class);
        sampleRepository = mock(SampleTestRepository.class);
        alertRepository = mock(EpiAlertRepository.class);
        service = new ObsEventLineAppService(obsRepository, taskRepository, siteRepository,
                stationRepository, reportRepository, sampleRepository, alertRepository);
    }

    /** 观测编号查不到（含已作废被 @TableLogic 滤掉）：回空线，不报错，也不往下查。 */
    @Test
    void trace_obsNotFound_returnsEmptyAndDoesNotChain() {
        when(obsRepository.findByObsNo("WO-2026-999999")).thenReturn(Mono.empty());

        List<EventLineItem> items = service.trace("WO-2026-999999").block();

        assertThat(items).isEmpty();
        verifyNoInteractions(taskRepository, siteRepository, stationRepository,
                reportRepository, sampleRepository, alertRepository);
    }

    /** 入参空白直接回空线，连库都不查。 */
    @Test
    void trace_blankNo_returnsEmpty() {
        assertThat(service.trace("  ").block()).isEmpty();
        verifyNoInteractions(obsRepository);
    }

    /** 一条完整链：站/点/任务/观测/上报/两样本（同刻，按 id 升序）/预警，按时刻倒序全带出。 */
    @Test
    void trace_fullChain_ordersByTimeDescendingWithStableTieBreak() {
        long stationId = 1L;
        long siteId = 2L;
        long taskId = 3L;
        long obsId = 4L;
        long reportId = 5L;

        // 时间从前到后：站登记 → 点登记 → 任务计划（今天 00:00）→ 观测 → 上报 → 两样本同刻送检 → 预警发布。
        // 任务计划日取今天：领域工厂不收早于今天的计划日，今天 00:00 仍早于今天的观测时刻，次序不受影响。
        LocalDateTime tStation = LocalDateTime.now().minusDays(10);
        LocalDateTime tSite = LocalDateTime.now().minusDays(9);
        LocalDate plannedDate = LocalDate.now();
        LocalDateTime tObs = plannedDate.atTime(9, 30);
        LocalDateTime tReport = plannedDate.atTime(10, 0);
        LocalDateTime tSample = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tAlert = LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0);

        MonitorStation station = MonitorStation.create("某站", MonitorStation.LEVEL_COUNTY, "某辖区", null, null);
        station.setId(stationId);
        station.setStationNo("ST-2026-0001");
        station.setCreateTime(tStation);

        MonitorSite site = MonitorSite.create(stationId, MonitorSite.TYPE_CAMERA, MonitorSite.HABITAT_FOREST, null);
        site.setId(siteId);
        site.setSiteNo("MP-2026-0001");
        site.setCreateTime(tSite);

        PatrolTask task = PatrolTask.create(stationId, siteId, PatrolTask.TYPE_ROUTINE, plannedDate, "张三");
        task.setId(taskId);
        task.setTaskNo("PT-2026-0001");

        WildlifeObs obs = WildlifeObs.create(taskId, siteId, "SP-0001", "COMMON", 1,
                ObsSummary.HEALTH_SUSPECT, tObs, "张三");
        obs.setId(obsId);
        obs.setObsNo("WO-2026-000001");

        AbnormalReport report = AbnormalReport.create(obsId, siteId, AbnormalReport.CATEGORY_SUSPECT_DISEASE,
                ObsSummary.HEALTH_SUSPECT, "COMMON", tReport);
        report.setId(reportId);
        report.setReportNo("AR-2026-0001");

        // 两份样本同刻送检：id 大的先建（300 > 250），同刻应按 id 升序 → 250（SM-...0001）在前。
        SampleTest sampleA = SampleTest.create(reportId, SampleTest.TYPE_SWAB, tSample, null, null);
        sampleA.setId(300L);
        sampleA.setSampleNo("SM-2026-0002");
        SampleTest sampleB = SampleTest.create(reportId, SampleTest.TYPE_BLOOD, tSample, null, null);
        sampleB.setId(250L);
        sampleB.setSampleNo("SM-2026-0001");

        EpiAlert alert = EpiAlert.raise(reportId, 300L, AbnormalReport.SEVERITY_HIGH, "COMMON");
        alert.setId(6L);
        alert.setAlertNo("AL-2026-0001");
        alert.setRaisedAt(tAlert);
        alert.advance(EpiAlert.STATUS_HANDLING, "现场封控", null); // 处置过也照样带出

        when(obsRepository.findByObsNo("WO-2026-000001")).thenReturn(Mono.just(obs));
        when(taskRepository.findById(taskId)).thenReturn(Mono.just(task));
        when(siteRepository.findById(siteId)).thenReturn(Mono.just(site));
        when(stationRepository.findById(stationId)).thenReturn(Mono.just(station));
        when(reportRepository.findActiveByObsId(obsId)).thenReturn(Mono.just(List.of(report)));
        when(sampleRepository.findActiveByReportId(reportId))
                .thenReturn(Mono.just(List.of(sampleA, sampleB)));
        when(alertRepository.findActiveByReportId(reportId)).thenReturn(Mono.just(List.of(alert)));

        List<EventLineItem> items = service.trace("WO-2026-000001").block();

        assertThat(items).hasSize(8);
        // 倒序：预警 → 样本（同刻按 id 升序）→ 上报 → 观测 → 任务 → 点 → 站。
        assertThat(items).extracting(EventLineItem::segment).containsExactly(
                EventSegment.ALERT,
                EventSegment.SAMPLE, EventSegment.SAMPLE,
                EventSegment.REPORT,
                EventSegment.OBS,
                EventSegment.TASK,
                EventSegment.SITE,
                EventSegment.STATION);
        // 每段都把自己的编号回出来。
        assertThat(items).extracting(EventLineItem::bizNo).containsExactly(
                "AL-2026-0001",
                "SM-2026-0001", "SM-2026-0002",
                "AR-2026-0001",
                "WO-2026-000001",
                "PT-2026-0001",
                "MP-2026-0001",
                "ST-2026-0001");
        // 时刻确实倒序；任务段落在计划日期当天 00:00。
        assertThat(items.get(0).eventTime()).isEqualTo(tAlert);
        assertThat(items.get(5).eventTime()).isEqualTo(plannedDate.atStartOfDay());
        // 样本没录结果时状态是待检（发生过的一段照留），预警处置中状态原样带出。
        assertThat(items.get(1).status()).isEqualTo(SampleTest.RESULT_PENDING);
        assertThat(items.get(0).status()).isEqualTo(EpiAlert.STATUS_HANDLING);
    }

    /** 观测在册但没有上报：按时刻倒序回三段（任务计划日在未来排最前），样本和预警仓储压根不被调。 */
    @Test
    void trace_noReport_onlyUpstreamAndObs() {
        long stationId = 1L;
        long siteId = 2L;
        long taskId = 3L;
        long obsId = 4L;
        // 任务计划日在两天后：纯按时刻摆，它最近，顶在观测前 —— 排序只认时刻，不替业务解释先后。
        LocalDate plannedDate = LocalDate.now().plusDays(2);

        MonitorSite site = MonitorSite.create(stationId, MonitorSite.TYPE_TRANSECT,
                MonitorSite.HABITAT_WETLAND, null);
        site.setId(siteId);
        site.setSiteNo("MP-2026-0009");
        site.setCreateTime(LocalDateTime.now().minusDays(5));

        PatrolTask task = PatrolTask.create(stationId, siteId, PatrolTask.TYPE_ROUTINE, plannedDate, null);
        task.setId(taskId);
        task.setTaskNo("PT-2026-0009");

        WildlifeObs obs = WildlifeObs.create(taskId, siteId, "SP-0001", "COMMON", 2,
                ObsSummary.HEALTH_NORMAL, LocalDateTime.now().minusDays(1), null);
        obs.setId(obsId);
        obs.setObsNo("WO-2026-000009");

        when(obsRepository.findByObsNo("WO-2026-000009")).thenReturn(Mono.just(obs));
        when(taskRepository.findById(taskId)).thenReturn(Mono.just(task));
        when(siteRepository.findById(siteId)).thenReturn(Mono.just(site));
        when(stationRepository.findById(stationId)).thenReturn(Mono.empty());
        when(reportRepository.findActiveByObsId(obsId)).thenReturn(Mono.just(List.of()));

        List<EventLineItem> items = service.trace("WO-2026-000009").block();

        assertThat(items).hasSize(3);
        assertThat(items).extracting(EventLineItem::segment)
                .containsExactly(EventSegment.TASK, EventSegment.OBS, EventSegment.SITE);
        // 站撤了（查不到）不影响其余段；没有上报时不往样本/预警那头查。
        verifyNoInteractions(sampleRepository, alertRepository);
    }

    /** 任务、点位都查不到（已取消/已撤）时不报错，观测这一段照样回得出。 */
    @Test
    void trace_taskAndSiteMissing_stillReturnsObsOnly() {
        WildlifeObs obs = WildlifeObs.create(30L, 20L, "SP-0001", "COMMON", 1,
                ObsSummary.HEALTH_NORMAL, LocalDateTime.now(), null);
        obs.setId(40L);
        obs.setObsNo("WO-2026-000010");

        when(obsRepository.findByObsNo("WO-2026-000010")).thenReturn(Mono.just(obs));
        when(taskRepository.findById(30L)).thenReturn(Mono.empty());
        when(siteRepository.findById(20L)).thenReturn(Mono.empty());
        when(reportRepository.findActiveByObsId(40L)).thenReturn(Mono.just(List.of()));

        List<EventLineItem> items = service.trace("WO-2026-000010").block();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).segment()).isEqualTo(EventSegment.OBS);
    }
}
