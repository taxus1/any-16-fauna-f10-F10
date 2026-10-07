package com.somepro.infrastructure.persistence.site;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.site.po.MonitorSitePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 监测点的 MyBatis-Plus Mapper（基础设施层）。
 *
 * 阻塞（JDBC）API，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface MonitorSiteMapper extends BaseMapper<MonitorSitePO> {

    /**
     * 查指定号段内已用的最大序号（编号生成用）。
     * 自定义 @Select 不拼 del_flag 条件：已撤点位占用的编号也不复用。
     */
    @Select("SELECT MAX(CAST(SUBSTRING(site_no, #{seqStart}) AS UNSIGNED)) "
            + "FROM t_monitor_site WHERE site_no LIKE CONCAT(#{prefix}, '%')")
    Long selectMaxSeq(@Param("prefix") String prefix, @Param("seqStart") int seqStart);

    /**
     * 按 id 查点位（含已撤点的）—— 事件线倒查专用。
     * 自定义 @Select 不拼 del_flag：已撤的点也得能倒出来，当初是在这个点上巡的。
     */
    @Select("SELECT * FROM t_monitor_site WHERE id = #{id}")
    MonitorSitePO selectAnyById(@Param("id") Long id);
}
