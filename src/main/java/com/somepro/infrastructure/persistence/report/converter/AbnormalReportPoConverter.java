package com.somepro.infrastructure.persistence.report.converter;

import com.somepro.domain.report.model.AbnormalReport;
import com.somepro.infrastructure.persistence.report.po.AbnormalReportPO;

/**
 * AbnormalReportPO（表）↔ AbnormalReport（领域）转换器（基础设施层）。
 *
 * 推进走 MyBatis-Plus 非空策略的条件更新，只有状态参与 SET，处置时刻由审计列记下。
 */
public final class AbnormalReportPoConverter {

    private AbnormalReportPoConverter() {
    }

    public static AbnormalReportPO toPo(AbnormalReport domain) {
        AbnormalReportPO po = new AbnormalReportPO();
        po.setId(domain.getId());
        po.setReportNo(domain.getReportNo());
        po.setObsId(domain.getObsId());
        po.setSiteId(domain.getSiteId());
        po.setCategory(domain.getCategory());
        po.setSeverity(domain.getSeverity());
        po.setStatus(domain.getStatus());
        po.setReportedAt(domain.getReportedAt());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static AbnormalReport toDomain(AbnormalReportPO po) {
        AbnormalReport domain = new AbnormalReport();
        domain.setId(po.getId());
        domain.setReportNo(po.getReportNo());
        domain.setObsId(po.getObsId());
        domain.setSiteId(po.getSiteId());
        domain.setCategory(po.getCategory());
        domain.setSeverity(po.getSeverity());
        domain.setStatus(po.getStatus());
        domain.setReportedAt(po.getReportedAt());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
