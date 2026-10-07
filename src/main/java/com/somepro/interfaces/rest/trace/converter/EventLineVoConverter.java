package com.somepro.interfaces.rest.trace.converter;

import com.somepro.domain.trace.model.EventLineItem;
import com.somepro.interfaces.rest.trace.vo.EventLineItemVO;
import com.somepro.interfaces.rest.trace.vo.ObsEventLineVO;

import java.util.List;

/**
 * 事件线读模型（领域）→ VO（对外）转换器（用户接口层）。纯字段摊平，不含业务。
 */
public final class EventLineVoConverter {

    private EventLineVoConverter() {
    }

    public static ObsEventLineVO toVo(String obsNo, List<EventLineItem> items) {
        List<EventLineItemVO> vos = items.stream()
                .map(EventLineVoConverter::toItemVo)
                .toList();
        return new ObsEventLineVO(obsNo, vos);
    }

    public static EventLineItemVO toItemVo(EventLineItem item) {
        return new EventLineItemVO(item.segment(), item.refId(), item.bizNo(),
                item.title(), item.status(), item.eventTime());
    }
}
