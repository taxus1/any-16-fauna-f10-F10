package com.somepro.interfaces.rest.sample;

import com.somepro.application.sample.SampleTestAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.sample.converter.SampleVoConverter;
import com.somepro.interfaces.rest.sample.vo.SampleCreateRequest;
import com.somepro.interfaces.rest.sample.vo.SampleResultRequest;
import com.somepro.interfaces.rest.sample.vo.SampleVO;
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
 * 采样送检与检测接口（用户接口层）：只做协议适配与 VO 转换，业务编排交给应用层。
 *
 * 样本分页：上报/样本类型/结果条件都可空，全空时翻整份在册样本；每行都带样本编号。
 */
@RestController
@RequestMapping("/api/samples")
public class SampleTestController {

    private final SampleTestAppService sampleAppService;

    public SampleTestController(SampleTestAppService sampleAppService) {
        this.sampleAppService = sampleAppService;
    }

    /** 登记采样：编号 SM-YYYY-NNNN 由服务端生成；上报得还在办才采得了；立起来落在待检。 */
    @PostMapping
    public Mono<Result<SampleVO>> register(@Valid @RequestBody SampleCreateRequest req) {
        return sampleAppService.register(req.reportId(), req.sampleType(), req.sentAt(),
                        req.labName(), req.testItem())
                .map(SampleVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<SampleVO>> detail(@PathVariable Long id) {
        return sampleAppService.detail(id)
                .map(SampleVoConverter::toVo)
                .map(Result::ok);
    }

    /** 检测结果回填：结果只录一次；录进去的同时上报从在办推到已采样，两头一起动。 */
    @PostMapping("/{id}/result")
    public Mono<Result<SampleVO>> recordResult(@PathVariable Long id,
                                               @Valid @RequestBody SampleResultRequest req) {
        return sampleAppService.recordResult(id, req.result(), req.testedAt())
                .map(SampleVoConverter::toVo)
                .map(Result::ok);
    }

    /** 样本分页：reportId/sampleType/result 条件随意拼，全空翻整份在册样本。 */
    @GetMapping({"", "/list"})
    public Mono<Result<PageVO<SampleVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String result) {
        return sampleAppService.pageSamples(pageNum, pageSize, reportId, sampleType, result)
                .map(SampleVoConverter::toPageVo)
                .map(Result::ok);
    }
}
