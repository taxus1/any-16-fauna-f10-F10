package com.somepro.infrastructure.persistence.sample;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.sample.po.SampleTestPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采样送检与检测的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上调用（见 SampleTestRepositoryImpl#blocking）。
 */
@Mapper
public interface SampleTestMapper extends BaseMapper<SampleTestPO> {

    /**
     * 查指定号段内已用的最大序号（编号生成用）。
     *
     * 自定义 @Select 不会被 @TableLogic 自动拼 del_flag 条件 —— 这是有意的：
     * 已删除样本占用的编号也不复用，一个号永远只归一条样本。
     *
     * @param prefix   编号前缀（如 "SM-2026-"）
     * @param seqStart 序号在编号串中的起始位置（SQL SUBSTRING 从 1 开始，即 prefix 长度 + 1）
     */
    @Select("SELECT MAX(CAST(SUBSTRING(sample_no, #{seqStart}) AS UNSIGNED)) "
            + "FROM t_sample_test WHERE sample_no LIKE CONCAT(#{prefix}, '%')")
    Long selectMaxSeq(@Param("prefix") String prefix, @Param("seqStart") int seqStart);
}
