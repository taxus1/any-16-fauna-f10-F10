package com.somepro.interfaces.rest.report;

import com.somepro.application.report.AbnormalReportAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.report.converter.ReportVoConverter;
import com.somepro.interfaces.rest.report.vo.ReportAdvanceRequest;
import com.somepro.interfaces.rest.report.vo.ReportCreateRequest;
import com.somepro.interfaces.rest.report.vo.ReportVO;
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
 * 异常个体上报接口（用户接口层）：只做协议适配与 VO 转换，业务编排交给应用层。
 *
 * 上报分页：点位/类别/严重程度/状态条件都可空，全空时翻整份在册上报；每行都带上报编号。
 * 已作废（逻辑删除）的上报不出现在分页与详情里，账仍留在库里。
 */
@RestController
@RequestMapping("/api/reports")
public class AbnormalReportController {

    private final AbnormalReportAppService reportAppService;

    public AbnormalReportController(AbnormalReportAppService reportAppService) {
        this.reportAppService = reportAppService;
    }

    /** 登记上报：编号 AR-YYYY-NNNN 由服务端生成；严重程度由系统按来头算；立起来落在已上报。 */
    @PostMapping
    public Mono<Result<ReportVO>> report(@Valid @RequestBody ReportCreateRequest req) {
        return reportAppService.report(req.obsId(), req.category(), req.reportedAt())
                .map(ReportVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<ReportVO>> detail(@PathVariable Long id) {
        return reportAppService.detail(id)
                .map(ReportVoConverter::toVo)
                .map(Result::ok);
    }

    /** 处置推进：已上报→处置中→已救护/已采样→已结案，只能顺着走；推进时记下处置时刻。 */
    @PostMapping("/{id}/advance")
    public Mono<Result<ReportVO>> advance(@PathVariable Long id,
                                          @Valid @RequestBody ReportAdvanceRequest req) {
        return reportAppService.advance(id, req.targetStatus())
                .map(ReportVoConverter::toVo)
                .map(Result::ok);
    }

    /** 作废：逻辑删除，名单里不再翻到，账留在库里；作废后该观测可重报。 */
    @PostMapping("/{id}/void")
    public Mono<Result<Void>> voidReport(@PathVariable Long id) {
        return reportAppService.voidReport(id)
                .then(Mono.just(Result.ok()));
    }

    /** 上报分页：siteId/category/severity/status 条件随意拼，全空翻整份在册上报。 */
    @GetMapping({"", "/list"})
    public Mono<Result<PageVO<ReportVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        return reportAppService.pageReports(pageNum, pageSize, siteId, category, severity, status)
                .map(ReportVoConverter::toPageVo)
                .map(Result::ok);
    }
}
