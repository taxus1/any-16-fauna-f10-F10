package com.somepro.infrastructure.persistence.alert;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.alert.po.EpiAlertPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 疫病预警与处置的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上调用（见 EpiAlertRepositoryImpl#blocking）。
 */
@Mapper
public interface EpiAlertMapper extends BaseMapper<EpiAlertPO> {

    /**
     * 锁定读（SELECT ... FOR UPDATE）查指定号段内当前最大的一条编号，供编号生成取号用。
     *
     * 为什么不用普通 SELECT MAX(...)：预警是在样本结果回填的大事务（REPEATABLE READ）里
     * 立的，事务内一致读的快照在首次读取时就固定了 —— 撞号重试再查还是事务开始时的旧值，
     * 会反复提出同一个已占用的号直到重试耗尽；并发事务的重复键检查锁叠在一起还会搅出死锁。
     * 锁定读是当前读，每次都能看到别的事务刚提交的号；并发取号在这条锁上排队，
     * 各取新号，不甩底层冲突。必须在事务里调用，锁随事务提交/回滚释放。
     *
     * 自定义 @Select 不会被 @TableLogic 自动拼 del_flag 条件 —— 这是有意的：
     * 已删除预警占用的编号也不复用，一个号永远只归一条预警。
     *
     * @param prefix   编号前缀（如 "AL-2026-"）
     * @param seqStart 序号在编号串中的起始位置（SQL SUBSTRING 从 1 开始，即 prefix 长度 + 1）
     * @return 当前最大编号（如 AL-2026-0007），号段为空时返回 null
     */
    @Select("SELECT alert_no FROM t_epi_alert WHERE alert_no LIKE CONCAT(#{prefix}, '%') "
            + "ORDER BY CAST(SUBSTRING(alert_no, #{seqStart}) AS UNSIGNED) DESC LIMIT 1 FOR UPDATE")
    String selectMaxNoForUpdate(@Param("prefix") String prefix, @Param("seqStart") int seqStart);
}
