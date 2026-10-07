package com.somepro.interfaces.rest.trace.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 观测事件线对外返回对象（VO）—— 不可变 record。
 *
 * obsNo 回显入参编号；items 是按事情发生先后倒着摆的各段（最近顶最前）。
 * 观测编号查不到（含已作废观测）时 items 为空列表、不报错 —— 查无此线就是一条空线。
 */
public record ObsEventLineVO(String obsNo, List<EventLineItemVO> items) implements Serializable {
}
