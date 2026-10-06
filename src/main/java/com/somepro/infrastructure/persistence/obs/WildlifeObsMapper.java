package com.somepro.infrastructure.persistence.obs;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.obs.po.WildlifeObsPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 野生动物观测记录的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上调用（见 WildlifeObsRepositoryImpl#blocking）。
 */
@Mapper
public interface WildlifeObsMapper extends BaseMapper<WildlifeObsPO> {

    /**
     * 查指定号段内已用的最大序号（编号生成用）。
     *
     * 自定义 @Select 不会被 @TableLogic 自动拼 del_flag 条件 —— 这是有意的：
     * 已作废（销账）观测占用的编号也不复用，一个号永远只归一条观测。
     *
     * @param prefix   编号前缀（如 "WO-2026-"）
     * @param seqStart 序号在编号串中的起始位置（SQL SUBSTRING 从 1 开始，即 prefix 长度 + 1）
     */
    @Select("SELECT MAX(CAST(SUBSTRING(obs_no, #{seqStart}) AS UNSIGNED)) "
            + "FROM t_wildlife_obs WHERE obs_no LIKE CONCAT(#{prefix}, '%')")
    Long selectMaxSeq(@Param("prefix") String prefix, @Param("seqStart") int seqStart);

    /**
     * 锁住一条观测记录（SELECT ... FOR UPDATE），供异常上报登记时把「同一观测只落一条
     * 未作废上报」串行化：并发上报在锁上排队，前面那单落库提交后，后面这单才数得到它。
     *
     * 自定义 @Select 不拼 del_flag：作废行的锁也取得到（作废观测早被应用层拦掉，这里只为取锁）。
     * 必须在事务里调用，行锁随事务提交/回滚释放。
     */
    @Select("SELECT id FROM t_wildlife_obs WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
