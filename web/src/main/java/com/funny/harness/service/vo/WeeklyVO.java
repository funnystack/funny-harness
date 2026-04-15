package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 周报看板 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class WeeklyVO {

    /** 周汇总（7 天 daily_summary 合计） */
    private Map<String, Object> summary;

    /** DAU 趋势折线，每项含 date、dau */
    private List<Map<String, Object>> dauTrend;

    /** 能力调用趋势，每项含 date、capability_name、call_count */
    private List<Map<String, Object>> capabilityTrend;

    /** 本周新增用户列表 */
    private List<Map<String, Object>> newUsers;
}
