package com.somepro.application.trace;

import com.somepro.domain.alert.model.EpiAlert;
import com.somepro.domain.alert.repository.EpiAlertRepository;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 观测事件线倒查应用层：输入一个观测编号（obsNo），回一份从头到尾的事件线。
 *
 * 这条线从观测往上、往下各倒一头，一段不缺：
 * - 往上：观测所属的监测站、监测点，观测挂的巡护任务；
 * - 本身：这条观测；
 * - 往下：由观测发起的异常上报、上报下送的样本、样本触发的预警。
 * 每段都带自己的业务编号（站点/点位/任务/观测/上报/样本/预警编号），好一段段对得上。
 *
 * 口径：
 * - 观测编号压根查不到的、或观测自己已作废（del_flag=1）的，回一份空线，不报错；
 *   已作废观测也不再拿它的号往下查（仓储按编号查时 @TableLogic 已把它滤掉）。
 * - 上报/样本/预警只串在册（未销账）的：已作废上报、已删除样本、已删除预警不进链；
 *   没结案的、处置过的、解除过的、结果还没出的（待检）都原样留着，不当没发生。
 * - 往上的站/点/任务照各模块在册封底：已撤的点、已取消的任务（逻辑删除）翻不到就不串，
 *   不因为其中一段缺失就把整条线报错 —— 剩下能对上的段照回。
 *
 * 排序：按各段那件事发生的时刻倒着摆，最近的顶最前；时刻相同的按链路上越靠后的段越靠前
 * （{@link EventSegment#rankOf}），再相同按段 id 升序兜底，次序稳定、可重复。
 */
@Service
public class ObsEventLineAppService {

    private final WildlifeObsRepository obsRepository;
    private final PatrolTaskRepository taskRepository;
    private final MonitorSiteRepository siteRepository;
    private final MonitorStationRepository stationRepository;
    private final AbnormalReportRepository reportRepository;
    private final SampleTestRepository sampleRepository;
    private final EpiAlertRepository alertRepository;

    public ObsEventLineAppService(WildlifeObsRepository obsRepository,
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
     * 倒一条观测的事件线。
     *
     * @param obsNo 观测编号（如 WO-2026-000001）；空白、查不到或已作废都回空线
     * @return 排好序的事件线段列表；查无此观测时为空列表，不抛错
     */
    public Mono<List<EventLineItem>> trace(String obsNo) {
        if (obsNo == null || obsNo.isBlank()) {
            return Mono.just(List.of());
        }
        String normalized = obsNo.trim();
        // 入口先按编号翻在册观测：查不到（含已作废）整条线就是空的，不再往下拿号查。
        return obsRepository.findByObsNo(normalized)
                .flatMap(obs -> loadUpstream(obs)
                        .flatMap(items -> loadDownstream(obs, items)))
                .map(ObsEventLineAppService::sort)
                .defaultIfEmpty(List.of());
    }

    /** 往上倒：巡护任务、监测点、监测站（都走各模块在册查询，缺哪段就少哪段，不报错）。 */
    private Mono<List<EventLineItem>> loadUpstream(WildlifeObs obs) {
        List<EventLineItem> items = new ArrayList<>();
        items.add(toObsItem(obs));
        Mono<Optional<PatrolTask>> taskMono = taskRepository.findById(obs.getTaskId())
                .map(Optional::of).defaultIfEmpty(Optional.empty());
        Mono<Optional<MonitorSite>> siteMono = siteRepository.findById(obs.getSiteId())
                .map(Optional::of).defaultIfEmpty(Optional.empty());
        // 站得先有点才知道站号：任务和点各自先查回来，站按两者带上的 stationId 取。
        return taskMono.flatMap(taskOpt -> siteMono.flatMap(siteOpt -> {
            taskOpt.map(ObsEventLineAppService::toTaskItem).ifPresent(items::add);
            siteOpt.map(ObsEventLineAppService::toSiteItem).ifPresent(items::add);
            Long stationId = taskOpt.map(PatrolTask::getStationId)
                    .or(() -> siteOpt.map(MonitorSite::getStationId))
                    .orElse(null);
            if (stationId == null) {
                return Mono.just(items);
            }
            return stationRepository.findById(stationId)
                    .map(station -> {
                        items.add(toStationItem(station));
                        return items;
                    })
                    .defaultIfEmpty(items);
        }));
    }

    /** 往下倒：观测 → 在册上报 → 在册样本 → 在册预警；已销账的由仓储过滤掉，不串进来。 */
    private Mono<List<EventLineItem>> loadDownstream(WildlifeObs obs, List<EventLineItem> items) {
        return reportRepository.findActiveByObsId(obs.getId())
                .flatMap(reports -> {
                    if (reports.isEmpty()) {
                        return Mono.just(items);
                    }
                    reports.forEach(report -> items.add(toReportItem(report)));
                    return Mono.when(
                                    reports.stream().map(report -> appendSamplesAndAlerts(report, items)).toList())
                            .thenReturn(items);
                });
    }

    /** 一份上报下的全部在册样本，以及这条上报链上的全部在册预警（同事务里立的、不同样本触发的都收）。 */
    private Mono<Void> appendSamplesAndAlerts(AbnormalReport report, List<EventLineItem> items) {
        Mono<List<SampleTest>> samplesMono = sampleRepository.findActiveByReportId(report.getId());
        Mono<List<EpiAlert>> alertsMono = alertRepository.findActiveByReportId(report.getId());
        return Mono.zip(samplesMono, alertsMono)
                .doOnNext(tuple -> {
                    tuple.getT1().forEach(sample -> items.add(toSampleItem(sample)));
                    tuple.getT2().forEach(alert -> items.add(toAlertItem(alert)));
                })
                .then();
    }

    /** 排序：发生时刻倒序（最近顶最前）；同刻按链路段优先级倒序，再同按 id 升序兜底。 */
    private static List<EventLineItem> sort(List<EventLineItem> items) {
        List<EventLineItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .comparing(EventLineItem::eventTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> EventSegment.rankOf(item.segment()), Comparator.reverseOrder())
                .thenComparing(item -> item.refId() == null ? Long.MAX_VALUE : item.refId()));
        return sorted;
    }

    private static EventLineItem toStationItem(MonitorStation station) {
        return EventLineItem.station(station.getId(), station.getStationNo(), station.getName(),
                station.getStatus(), station.getCreateTime());
    }

    private static EventLineItem toSiteItem(MonitorSite site) {
        String title = site.getSiteNo() + " " + typeText(site.getSiteType())
                + "（" + habitatText(site.getHabitat()) + "）";
        return EventLineItem.site(site.getId(), site.getSiteNo(), title,
                site.getStatus(), site.getCreateTime());
    }

    private static EventLineItem toTaskItem(PatrolTask task) {
        String title = task.getTaskNo() + " " + patrolTypeText(task.getPatrolType())
                + (task.getExecutor() != null ? " 执行人：" + task.getExecutor() : "");
        // 任务这件「事」按计划巡护日期算时刻；排期字段是 DATE，落到当天 00:00。
        return EventLineItem.task(task.getId(), task.getTaskNo(), title,
                task.getStatus(),
                task.getPlannedDate() != null ? task.getPlannedDate().atStartOfDay() : null);
    }

    private static EventLineItem toObsItem(WildlifeObs obs) {
        String title = obs.getSpeciesCode() + " ×" + obs.getIndividualCount()
                + (obs.getRecorder() != null ? " 记录人：" + obs.getRecorder() : "");
        return EventLineItem.obs(obs.getId(), obs.getObsNo(), title,
                obs.getHealthStatus(), obs.getObservedAt());
    }

    private static EventLineItem toReportItem(AbnormalReport report) {
        String title = categoryText(report.getCategory()) + " 严重程度："
                + severityText(report.getSeverity());
        return EventLineItem.report(report.getId(), report.getReportNo(), title,
                report.getStatus(), report.getReportedAt());
    }

    private static EventLineItem toSampleItem(SampleTest sample) {
        String title = sampleTypeText(sample.getSampleType())
                + (sample.getLabName() != null ? " 检测机构：" + sample.getLabName() : "")
                + (sample.getTestItem() != null ? " 检测项目：" + sample.getTestItem() : "");
        // 状态位带检测结果：送检后结果没出（待检）也是发生过的一段，照留。
        return EventLineItem.sample(sample.getId(), sample.getSampleNo(), title,
                sample.getResult(), sample.getSentAt());
    }

    private static EventLineItem toAlertItem(EpiAlert alert) {
        String title = "预警级别：" + alertLevelText(alert.getAlertLevel())
                + (alert.getDisposalMethod() != null ? " 处置：" + alert.getDisposalMethod() : "");
        return EventLineItem.alert(alert.getId(), alert.getAlertNo(), title,
                alert.getStatus(), alert.getRaisedAt());
    }

    private static String typeText(String type) {
        return switch (type) {
            case MonitorSite.TYPE_TRANSECT -> "样线";
            case MonitorSite.TYPE_SAMPLE_POINT -> "样点";
            case MonitorSite.TYPE_CAMERA -> "红外相机位";
            default -> type;
        };
    }

    private static String habitatText(String habitat) {
        return switch (habitat) {
            case MonitorSite.HABITAT_FOREST -> "林地";
            case MonitorSite.HABITAT_WETLAND -> "湿地";
            case MonitorSite.HABITAT_GRASSLAND -> "草地";
            case MonitorSite.HABITAT_FARMLAND -> "农田";
            case MonitorSite.HABITAT_DESERT -> "荒漠";
            default -> habitat;
        };
    }

    private static String patrolTypeText(String type) {
        return switch (type) {
            case PatrolTask.TYPE_ROUTINE -> "常规巡护";
            case PatrolTask.TYPE_SPECIAL -> "专项巡护";
            case PatrolTask.TYPE_EMERGENCY -> "应急巡护";
            default -> type;
        };
    }

    private static String categoryText(String category) {
        return switch (category) {
            case AbnormalReport.CATEGORY_INJURED -> "受伤";
            case AbnormalReport.CATEGORY_DEAD -> "死亡";
            case AbnormalReport.CATEGORY_SUSPECT_DISEASE -> "疑似疫病";
            default -> category;
        };
    }

    private static String severityText(String severity) {
        return switch (severity) {
            case AbnormalReport.SEVERITY_HIGH -> "高";
            case AbnormalReport.SEVERITY_MEDIUM -> "中";
            default -> severity;
        };
    }

    private static String sampleTypeText(String type) {
        return switch (type) {
            case SampleTest.TYPE_BLOOD -> "血液样本";
            case SampleTest.TYPE_SWAB -> "拭子样本";
            case SampleTest.TYPE_TISSUE -> "组织样本";
            case SampleTest.TYPE_FECES -> "粪便样本";
            default -> type;
        };
    }

    private static String alertLevelText(String level) {
        return switch (level) {
            case EpiAlert.LEVEL_BLUE -> "蓝色";
            case EpiAlert.LEVEL_YELLOW -> "黄色";
            case EpiAlert.LEVEL_ORANGE -> "橙色";
            case EpiAlert.LEVEL_RED -> "红色";
            default -> level;
        };
    }
}
