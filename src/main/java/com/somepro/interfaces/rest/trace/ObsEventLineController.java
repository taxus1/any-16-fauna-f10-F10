package com.somepro.interfaces.rest.trace;

import com.somepro.application.trace.ObsEventLineAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.trace.converter.EventLineVoConverter;
import com.somepro.interfaces.rest.trace.vo.ObsEventLineVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 观测事件线倒查接口（用户接口层）：只做协议适配与 VO 转换，编排交给应用层。
 *
 * 上级核账/异常复盘用：给一个观测编号，回从站、点、任务、观测、上报、样本到预警的整条线，
 * 各段带自己的编号，按发生先后倒着摆。观测编号查不到（或观测已作废）回空线，不报错。
 */
@RestController
@RequestMapping("/api/obs")
public class ObsEventLineController {

    private final ObsEventLineAppService eventLineAppService;

    public ObsEventLineController(ObsEventLineAppService eventLineAppService) {
        this.eventLineAppService = eventLineAppService;
    }

    /**
     * 倒查一条观测的事件线：路径段是观测业务编号（obsNo，如 WO-2026-000001），
     * 与按雪花 id 查详情的 GET /{id} 不冲突（多一段 /event-line）。
     */
    @GetMapping("/{obsNo}/event-line")
    public Mono<Result<ObsEventLineVO>> trace(@PathVariable String obsNo) {
        return eventLineAppService.trace(obsNo)
                .map(items -> EventLineVoConverter.toVo(obsNo, items))
                .map(Result::ok);
    }
}
