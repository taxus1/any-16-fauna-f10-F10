package com.somepro.interfaces.rest.obs;

import com.somepro.application.obs.WildlifeObsAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.obs.converter.ObsVoConverter;
import com.somepro.interfaces.rest.obs.vo.ObsCreateRequest;
import com.somepro.interfaces.rest.obs.vo.ObsUpdateRequest;
import com.somepro.interfaces.rest.obs.vo.ObsVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 野生动物观测接口（用户接口层）：只做协议适配与 VO 转换，业务编排交给应用层。
 *
 * 观测分页：任务/点位/物种/健康状态条件都可空，全空时翻整份在册观测；每行都带观测编号。
 * 已作废（逻辑删除）的记录不出现在分页与详情里，底子仍留在库里备查。
 */
@RestController
@RequestMapping("/api/obs")
public class WildlifeObsController {

    private final WildlifeObsAppService obsAppService;

    public WildlifeObsController(WildlifeObsAppService obsAppService) {
        this.obsAppService = obsAppService;
    }

    /** 录入观测：编号 WO-YYYY-NNNNNN 由服务端生成；任务必须正在执行、物种必须在名录且启用。 */
    @PostMapping
    public Mono<Result<ObsVO>> record(@Valid @RequestBody ObsCreateRequest req) {
        return obsAppService.record(req.taskId(), req.siteId(), req.speciesCode(),
                        req.individualCount(), req.healthStatus(), req.observedAt(), req.recorder())
                .map(ObsVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<ObsVO>> detail(@PathVariable Long id) {
        return obsAppService.detail(id)
                .map(ObsVoConverter::toVo)
                .map(Result::ok);
    }

    /** 改录：点位/物种/数量/健康状态/观测时刻/记录人传啥改啥，任务归属不改。 */
    @PutMapping("/{id}")
    public Mono<Result<ObsVO>> revise(@PathVariable Long id,
                                      @Valid @RequestBody ObsUpdateRequest req) {
        return obsAppService.revise(id, req.siteId(), req.speciesCode(), req.individualCount(),
                        req.healthStatus(), req.observedAt(), req.recorder())
                .map(ObsVoConverter::toVo)
                .map(Result::ok);
    }

    /** 作废：逻辑删除，清单里不再翻到，底子留在库里备查。 */
    @PostMapping("/{id}/void")
    public Mono<Result<Void>> voidObs(@PathVariable Long id) {
        return obsAppService.voidObs(id)
                .then(Mono.just(Result.ok()));
    }

    /**
     * 观测分页：taskId/siteId/speciesCode/healthStatus 条件随意拼，
     * 可带观测时刻区间（observedFrom 含、observedTo 不含，ISO 日期时间），全空翻整份在册观测。
     */
    @GetMapping({"", "/list"})
    public Mono<Result<PageVO<ObsVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String speciesCode,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo) {
        return obsAppService.pageObs(pageNum, pageSize, taskId, siteId, speciesCode,
                        healthStatus, observedFrom, observedTo)
                .map(ObsVoConverter::toPageVo)
                .map(Result::ok);
    }
}
