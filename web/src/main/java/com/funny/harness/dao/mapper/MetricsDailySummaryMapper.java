package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsDailySummaryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 每日指标汇总表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsDailySummaryMapper extends BaseMapper<MetricsDailySummaryDO> {

    // 日期范围汇总
    List<MetricsDailySummaryDO> sumSummaryByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
