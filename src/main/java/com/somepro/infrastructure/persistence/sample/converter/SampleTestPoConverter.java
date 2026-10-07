package com.somepro.infrastructure.persistence.sample.converter;

import com.somepro.domain.sample.model.SampleTest;
import com.somepro.infrastructure.persistence.sample.po.SampleTestPO;

/**
 * SampleTestPO（表）↔ SampleTest（领域）转换器（基础设施层）。
 *
 * 结果回填走 MyBatis-Plus 非空策略的条件更新，只有结果与检测时刻参与 SET。
 */
public final class SampleTestPoConverter {

    private SampleTestPoConverter() {
    }

    public static SampleTestPO toPo(SampleTest domain) {
        SampleTestPO po = new SampleTestPO();
        po.setId(domain.getId());
        po.setSampleNo(domain.getSampleNo());
        po.setReportId(domain.getReportId());
        po.setSampleType(domain.getSampleType());
        po.setSentAt(domain.getSentAt());
        po.setLabName(domain.getLabName());
        po.setTestItem(domain.getTestItem());
        po.setResult(domain.getResult());
        po.setTestedAt(domain.getTestedAt());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static SampleTest toDomain(SampleTestPO po) {
        SampleTest domain = new SampleTest();
        domain.setId(po.getId());
        domain.setSampleNo(po.getSampleNo());
        domain.setReportId(po.getReportId());
        domain.setSampleType(po.getSampleType());
        domain.setSentAt(po.getSentAt());
        domain.setLabName(po.getLabName());
        domain.setTestItem(po.getTestItem());
        domain.setResult(po.getResult());
        domain.setTestedAt(po.getTestedAt());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
