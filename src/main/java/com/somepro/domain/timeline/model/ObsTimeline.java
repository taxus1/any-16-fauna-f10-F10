package com.somepro.domain.timeline.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一条观测从头到尾的事件线（领域值对象）：上级核账或异常复盘时，
 * 拿一个观测编号把整条线倒出来 —— 从哪个站哪个点巡的、什么时候看见的、
 * 报了没有、送检没有、最后立没立预警，一段都不能缺。
 *
 * 摆法：按事情发生的先后倒着摆，最近的那件顶在最前（occurredAt 降序，
 * 时刻空了的垫底）；同一时刻的按段种在线上的先后（{@link SegmentKind} 声明序），
 * 再一样的保持来样次序 —— 排序是稳定的，仓储按 id 升序给的，早落库的排前。
 */
public record ObsTimeline(String obsNo, List<TimelineSegment> segments) {

    /** 段序：发生的晚的在前；时刻空了的垫底；同刻按段种声明序；再同序靠稳定排序保持来样次序。 */
    private static final Comparator<TimelineSegment> BY_OCCURRENCE =
            Comparator.comparing(TimelineSegment::occurredAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(segment -> segment.kind().ordinal());

    /** 归拢成线：段落排好序后冻结，不再变。 */
    public static ObsTimeline of(String obsNo, List<TimelineSegment> segments) {
        List<TimelineSegment> sorted = new ArrayList<>(segments);
        sorted.sort(BY_OCCURRENCE);
        return new ObsTimeline(obsNo, List.copyOf(sorted));
    }

    /** 空线：观测编号查不到（或观测自己已销掉）时回这个，不报错。 */
    public static ObsTimeline empty(String obsNo) {
        return new ObsTimeline(obsNo, List.of());
    }
}
