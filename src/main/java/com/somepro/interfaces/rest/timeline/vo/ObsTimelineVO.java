package com.somepro.interfaces.rest.timeline.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 观测事件线对外返回对象（VO，用户接口层）—— 不可变 record。
 *
 * segments 按事情发生的先后倒着摆：最近的那件顶在最前；同一时刻的按段种
 * 在线上的先后（站点→任务→观测→上报→样本→预警），再一样的按落库先后。
 * 观测编号查不到（或观测已销掉）时 segments 为空列表，不报错。
 */
public record ObsTimelineVO(String obsNo, List<TimelineSegmentVO> segments) implements Serializable {
}
