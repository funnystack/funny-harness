package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户指标表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsUserMapper extends BaseMapper<MetricsUserDO> {

    // 按日期范围查询新增用户
    List<Map<String, Object>> newUsersByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 项目×用户聚合热度矩阵
    List<Map<String, Object>> projectUserMatrix(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
