package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsAgentTraceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Agent执行轨迹表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsAgentTraceMapper extends BaseMapper<MetricsAgentTraceDO> {

    // 一次通过率（loop_count=1 的比例）
    BigDecimal firstPassRate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 按任务类型统计成本分布
    List<Map<String, Object>> costByTaskType(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 按agent_type聚合利用率
    List<Map<String, Object>> utilizationByAgentType(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 30天任务完成率趋势
    List<Map<String, Object>> completionRateTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
