package com.somepro.interfaces.rest.obs.converter;

import com.somepro.domain.obs.model.WildlifeObs;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.obs.vo.ObsVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WildlifeObs（领域）→ ObsVO（对外）转换器（用户接口层）。
 */
public final class ObsVoConverter {

    private ObsVoConverter() {
    }

    public static ObsVO toVo(WildlifeObs obs) {
        return new ObsVO(obs.getId(), obs.getObsNo(), obs.getTaskId(), obs.getSiteId(),
                obs.getSpeciesCode(), obs.getProtectionLevel(), obs.getIndividualCount(),
                obs.getHealthStatus(), obs.getObservedAt(), obs.getRecorder(), obs.getCreateTime());
    }

    public static PageVO<ObsVO> toPageVo(PageResult<WildlifeObs> page) {
        List<ObsVO> content = page.content().stream()
                .map(ObsVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
