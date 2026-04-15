package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsSessionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 会话指标表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsSessionMapper extends BaseMapper<MetricsSessionDO> {

    // 按小时聚合 Token 消耗（当天）
    List<Map<String, Object>> sumTokensByHour(@Param("date") String date);

    // 按项目聚合 Top N
    List<Map<String, Object>> topByProject(@Param("date") String date, @Param("limit") int limit);

    // 按用户聚合排行
    List<Map<String, Object>> topByUser(@Param("date") String date, @Param("limit") int limit);
}
