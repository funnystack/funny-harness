package com.funny.harness.dao.mapper;

import com.funny.harness.dao.entity.MetricsAlertDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 指标告警表 Mapper 接口
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Mapper
public interface MetricsAlertMapper extends BaseMapper<MetricsAlertDO> {

    // 按条件查询告警（支持 type/severity/日期范围过滤）
    List<MetricsAlertDO> queryAlerts(@Param("type") String type,
                                     @Param("severity") String severity,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate);
}
