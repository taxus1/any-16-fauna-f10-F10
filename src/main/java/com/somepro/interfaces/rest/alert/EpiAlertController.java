package com.somepro.interfaces.rest.alert;

import com.somepro.application.alert.EpiAlertAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.alert.converter.AlertVoConverter;
import com.somepro.interfaces.rest.alert.vo.AlertAdvanceRequest;
import com.somepro.interfaces.rest.alert.vo.AlertVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 疫病预警与处置接口（用户接口层）：只做协议适配与 VO 转换，业务编排交给应用层。
 *
 * 预警不靠人点：样本检测那条链上结果一录成阳性，预警自己跟着立起来，这里只提供
 * 处置推进、查看与翻页。预警分页：上报/样本/级别/状态条件都可空，全空时翻整份在册预警；
 * 每行都带预警编号。
 */
@RestController
@RequestMapping("/api/alerts")
public class EpiAlertController {

    private final EpiAlertAppService alertAppService;

    public EpiAlertController(EpiAlertAppService alertAppService) {
        this.alertAppService = alertAppService;
    }

    @GetMapping("/{id}")
    public Mono<Result<AlertVO>> detail(@PathVariable Long id) {
        return alertAppService.detail(id)
                .map(AlertVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 处置推进：已发布→处置中→已解除→已归档，只能顺着走；每推一步记下处置时刻。
     * 推进到已解除时记解除时刻（不能早于发布时刻），挂的那条上报还没结案的跟着推到已结案。
     */
    @PostMapping("/{id}/advance")
    public Mono<Result<AlertVO>> advance(@PathVariable Long id,
                                         @Valid @RequestBody AlertAdvanceRequest req) {
        return alertAppService.advance(id, req.targetStatus(), req.disposalMethod(), req.resolvedAt())
                .map(AlertVoConverter::toVo)
                .map(Result::ok);
    }

    /** 预警分页：reportId/sampleId/alertLevel/status 条件随意拼，全空翻整份在册预警。 */
    @GetMapping({"", "/list"})
    public Mono<Result<PageVO<AlertVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Long sampleId,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String status) {
        return alertAppService.pageAlerts(pageNum, pageSize, reportId, sampleId, alertLevel, status)
                .map(AlertVoConverter::toPageVo)
                .map(Result::ok);
    }
}
