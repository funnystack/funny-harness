package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsSkillUsageDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Skill使用统计表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsSkillUsageMapper extends BaseMapper<MetricsSkillUsageDO> {

    // 能力热度排行
    List<Map<String, Object>> capabilityHeatmap(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 7天能力调用趋势（按天+能力类型聚合）
    List<Map<String, Object>> capabilityTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 统计某时间段内最热 Skill 和调用总数
    Map<String, Object> topSkillAndCount(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
