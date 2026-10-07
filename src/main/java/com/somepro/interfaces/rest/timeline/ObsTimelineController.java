package com.somepro.interfaces.rest.timeline;

import com.somepro.application.timeline.ObsTimelineAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.timeline.converter.ObsTimelineVoConverter;
import com.somepro.interfaces.rest.timeline.vo.ObsTimelineVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 观测事件线接口（用户接口层）：只做协议适配与 VO 转换，倒链编排交给应用层。
 *
 * 输入一个观测编号，回一份从头到尾的事件线：站点（站+点）→任务→观测→上报→样本→预警，
 * 每段带自己的编号，按发生先后倒序（最近的顶最前）。编号查不到或观测已销掉的回空线，不报错。
 */
@RestController
@RequestMapping("/api/timelines")
public class ObsTimelineController {

    private final ObsTimelineAppService timelineAppService;

    public ObsTimelineController(ObsTimelineAppService timelineAppService) {
        this.timelineAppService = timelineAppService;
    }

    /** 观测事件线：按观测编号把整条线倒出来；编号查不到（或观测已销掉）回空线，不报错。 */
    @GetMapping("/obs/{obsNo}")
    public Mono<Result<ObsTimelineVO>> obsTimeline(@PathVariable String obsNo) {
        return timelineAppService.timeline(obsNo)
                .map(ObsTimelineVoConverter::toVo)
                .map(Result::ok);
    }
}
