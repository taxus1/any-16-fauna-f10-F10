package com.somepro.application.obs;

import com.somepro.common.exception.BizException;
import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.obs.repository.WildlifeObsRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.site.model.MonitorSite;
import com.somepro.domain.site.repository.MonitorSiteRepository;
import com.somepro.domain.species.model.Species;
import com.somepro.domain.species.repository.SpeciesRepository;
import com.somepro.domain.task.model.PatrolTask;
import com.somepro.domain.task.repository.PatrolTaskRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 野生动物观测应用层：编排观测用例（录入、修改、查看、作废、条件分页）。
 *
 * 录入两道前置（缺一不可）：
 * - 任务得正在执行（IN_PROGRESS）：待执行还没开工、已完成账已冻结、已取消已销账的任务，
 *   都不再往底下录观测；
 * - 物种得在名录里且处在启用状态（ENABLED）：编码查不到的、名录里停用掉的，一律不收，
 *   不允许拿别的编码糊弄。
 * 另外点位得是库里还在册的（监测点被撤掉的不能往上挂）。
 *
 * 保护级别快照：录入时照物种名录里该物种「当前写着的」级别抄一份进观测
 * （protection_level），抄进来就不跟着名录变；以后名录把级别调高调低，老观测仍是当初那份。
 * 修改观测时换了物种，快照照新物种当前级别重抄一份；没换物种，老快照原样保留。
 */
@Service
public class WildlifeObsAppService {

    private final WildlifeObsRepository obsRepository;
    private final PatrolTaskRepository taskRepository;
    private final MonitorSiteRepository siteRepository;
    private final SpeciesRepository speciesRepository;

    public WildlifeObsAppService(WildlifeObsRepository obsRepository,
                                 PatrolTaskRepository taskRepository,
                                 MonitorSiteRepository siteRepository,
                                 SpeciesRepository speciesRepository) {
        this.obsRepository = obsRepository;
        this.taskRepository = taskRepository;
        this.siteRepository = siteRepository;
        this.speciesRepository = speciesRepository;
    }

    /**
     * 当场录一条观测。健康状态不传默认正常（领域对象兜底），观测时刻不传取登记当下。
     * 编号由仓储层按 WO-YYYY-NNNNNN 生成；保护级别照名录当前值抄成快照。
     */
    public Mono<WildlifeObs> record(Long taskId, Long siteId, String speciesCode, Integer individualCount,
                                    String healthStatus, LocalDateTime observedAt, String recorder) {
        return requireOngoingTask(taskId)
                .then(requireExistingSite(siteId))
                .then(requireEnabledSpecies(speciesCode))
                .flatMap(species -> {
                    WildlifeObs obs = WildlifeObs.create(taskId, siteId, speciesCode,
                            species.getProtectionLevel(), individualCount, healthStatus, observedAt, recorder);
                    return obsRepository.create(obs);
                });
    }

    /**
     * 改录：点位/物种/数量/健康状态/观测时刻/记录人传啥改啥，任务归属不改。
     * 换点位要重新验点在册；换物种要重新验「在名录且启用」，并照新物种当前级别重抄快照。
     */
    public Mono<WildlifeObs> revise(Long id, Long siteId, String speciesCode, Integer individualCount,
                                    String healthStatus, LocalDateTime observedAt, String recorder) {
        return obsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("观测记录不存在")))
                .flatMap(obs -> {
                    Long targetSiteId = siteId != null ? siteId : obs.getSiteId();
                    String incomingCode = normalizeCode(speciesCode);
                    // 物种编码变了才需要重新验名录并重抄快照；没变就沿用老快照（不跟名录后续调整）
                    boolean speciesChanged = incomingCode != null
                            && !incomingCode.equals(obs.getSpeciesCode());
                    Mono<Species> speciesCheck = speciesChanged
                            ? requireEnabledSpecies(incomingCode)
                            : Mono.empty();
                    // 物种没变时 speciesCheck 是空 Mono：用 Optional 兜成「无新级别」，
                    // 保证后续 flatMap 仍会执行（空 Mono 直接 flatMap 会把更新整个吞掉）
                    return requireExistingSite(targetSiteId)
                            .then(speciesCheck.map(Optional::of).defaultIfEmpty(Optional.empty()))
                            .flatMap(newSpecies -> {
                                String snapshot = newSpecies.map(Species::getProtectionLevel).orElse(null);
                                obs.revise(siteId, incomingCode, snapshot,
                                        individualCount, healthStatus, observedAt, recorder);
                                return obsRepository.update(obs);
                            });
                });
    }

    /** 查看单条在册观测（已作废的翻不到，底子仍在库里）。 */
    public Mono<WildlifeObs> detail(Long id) {
        return obsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("观测记录不存在")));
    }

    /** 作废：逻辑删除（del_flag=1），清单里不再翻到，底子留在库里备查。 */
    public Mono<Void> voidObs(Long id) {
        return obsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("观测记录不存在")))
                .flatMap(obs -> obsRepository.voidObs(obs.getId()));
    }

    /** 条件分页：任务/点位/物种/健康状态随意拼，可带观测时刻区间，全空翻整份在册观测。 */
    public Mono<PageResult<WildlifeObs>> pageObs(int pageNum, int pageSize,
                                                 Long taskId, Long siteId, String speciesCode,
                                                 String healthStatus,
                                                 LocalDateTime observedFrom, LocalDateTime observedTo) {
        return obsRepository.page(pageNum, pageSize, taskId, siteId,
                normalizeCode(speciesCode), healthStatus, observedFrom, observedTo);
    }

    /** 任务必须存在且正在执行：待执行/已完成/已取消的任务都不再收新观测。 */
    private Mono<PatrolTask> requireOngoingTask(Long taskId) {
        if (taskId == null) {
            return Mono.error(new BizException("巡护任务不能为空"));
        }
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new BizException("巡护任务不存在")))
                .flatMap(task -> {
                    if (!PatrolTask.STATUS_IN_PROGRESS.equals(task.getStatus())) {
                        return Mono.error(new BizException("只有正在执行的巡护任务才能录入观测"));
                    }
                    return Mono.just(task);
                });
    }

    /** 点位必须存在（已撤掉/查不到的点不往上挂）；@TableLogic 自动过滤已删除点位。 */
    private Mono<MonitorSite> requireExistingSite(Long siteId) {
        if (siteId == null) {
            return Mono.error(new BizException("监测点不能为空"));
        }
        return siteRepository.findById(siteId)
                .switchIfEmpty(Mono.error(new BizException("监测点不存在")));
    }

    /** 物种必须在名录里且启用：编码查不到/停用的都不收。 */
    private Mono<Species> requireEnabledSpecies(String speciesCode) {
        String code = normalizeCode(speciesCode);
        if (code == null) {
            return Mono.error(new BizException("物种编码不能为空"));
        }
        return speciesRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new BizException("物种不在名录里，不能录入观测")))
                .flatMap(species -> {
                    if (!Species.STATUS_ENABLED.equals(species.getStatus())) {
                        return Mono.error(new BizException("物种已在名录中停用，不能再录入观测"));
                    }
                    return Mono.just(species);
                });
    }

    private static String normalizeCode(String speciesCode) {
        return (speciesCode == null || speciesCode.isBlank()) ? null : speciesCode.trim();
    }
}
