package com.somepro.infrastructure.persistence.report;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.report.po.AbnormalReportPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 异常个体上报的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上调用（见 AbnormalReportRepositoryImpl#blocking）。
 */
@Mapper
public interface AbnormalReportMapper extends BaseMapper<AbnormalReportPO> {

    /**
     * 查指定号段内已用的最大序号（编号生成用）。
     *
     * 自定义 @Select 不会被 @TableLogic 自动拼 del_flag 条件 —— 这是有意的：
     * 已作废上报占用的编号也不复用，一个号永远只归一条上报。
     *
     * @param prefix   编号前缀（如 "AR-2026-"）
     * @param seqStart 序号在编号串中的起始位置（SQL SUBSTRING 从 1 开始，即 prefix 长度 + 1）
     */
    @Select("SELECT MAX(CAST(SUBSTRING(report_no, #{seqStart}) AS UNSIGNED)) "
            + "FROM t_abnormal_report WHERE report_no LIKE CONCAT(#{prefix}, '%')")
    Long selectMaxSeq(@Param("prefix") String prefix, @Param("seqStart") int seqStart);

    /**
     * 按观测 id 列上报（含已作废的），按 id 升序 —— 事件线倒查专用。
     * 自定义 @Select 不拼 del_flag：销掉的上报本身不进线（调用方滤），
     * 但它名下送过的样本、立过的预警得顺着它往下找。
     */
    @Select("SELECT * FROM t_abnormal_report WHERE obs_id = #{obsId} ORDER BY id")
    List<AbnormalReportPO> selectAnyByObsId(@Param("obsId") Long obsId);
}
