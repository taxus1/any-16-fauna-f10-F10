package com.somepro.interfaces.rest.timeline.converter;

import com.somepro.domain.timeline.model.ObsTimeline;
import com.somepro.domain.timeline.model.TimelineSegment;
import com.somepro.interfaces.rest.alert.converter.AlertVoConverter;
import com.somepro.interfaces.rest.obs.converter.ObsVoConverter;
import com.somepro.interfaces.rest.report.converter.ReportVoConverter;
import com.somepro.interfaces.rest.sample.converter.SampleVoConverter;
import com.somepro.interfaces.rest.site.converter.SiteVoConverter;
import com.somepro.interfaces.rest.station.converter.StationVoConverter;
import com.somepro.interfaces.rest.task.converter.PatrolTaskVoConverter;
import com.somepro.interfaces.rest.timeline.vo.ObsTimelineVO;
import com.somepro.interfaces.rest.timeline.vo.TimelineSegmentVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ObsTimeline（领域）→ ObsTimelineVO（对外）转换器（用户接口层）。
 *
 * 每段复用各模块自己的 VO 转换器，编号口径与各模块台账保持一致；
 * 段落只填自己那摊字段，其余槽位空（JSON 里不出现）。
 */
public final class ObsTimelineVoConverter {

    private ObsTimelineVoConverter() {
    }

    public static ObsTimelineVO toVo(ObsTimeline timeline) {
        List<TimelineSegmentVO> segments = timeline.segments().stream()
                .map(ObsTimelineVoConverter::toSegmentVo)
                .collect(Collectors.toList());
        return new ObsTimelineVO(timeline.obsNo(), segments);
    }

    private static TimelineSegmentVO toSegmentVo(TimelineSegment segment) {
        return new TimelineSegmentVO(segment.kind().name(), segment.occurredAt(),
                segment.station() == null ? null : StationVoConverter.toVo(segment.station()),
                segment.site() == null ? null : SiteVoConverter.toVo(segment.site()),
                segment.task() == null ? null : PatrolTaskVoConverter.toVo(segment.task()),
                segment.obs() == null ? null : ObsVoConverter.toVo(segment.obs()),
                segment.report() == null ? null : ReportVoConverter.toVo(segment.report()),
                segment.sample() == null ? null : SampleVoConverter.toVo(segment.sample()),
                segment.alert() == null ? null : AlertVoConverter.toVo(segment.alert()));
    }
}
