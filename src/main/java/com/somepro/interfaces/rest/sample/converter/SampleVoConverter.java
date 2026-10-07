package com.somepro.interfaces.rest.sample.converter;

import com.somepro.domain.sample.model.SampleTest;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.sample.vo.SampleVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SampleTest（领域）→ SampleVO（对外）转换器（用户接口层）。
 */
public final class SampleVoConverter {

    private SampleVoConverter() {
    }

    public static SampleVO toVo(SampleTest sample) {
        return new SampleVO(sample.getId(), sample.getSampleNo(), sample.getReportId(),
                sample.getSampleType(), sample.getSentAt(), sample.getLabName(),
                sample.getTestItem(), sample.getResult(), sample.getTestedAt(),
                sample.getCreateTime());
    }

    public static PageVO<SampleVO> toPageVo(PageResult<SampleTest> page) {
        List<SampleVO> content = page.content().stream()
                .map(SampleVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
