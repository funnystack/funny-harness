package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 日报看板 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class DailyVO {

    /** 日汇总 */
    private Map<String, Object> summary;

    /** Token 小时趋势，每项含 hour、tokens */
    private List<Map<String, Object>> tokenHourlyTrend;

    /** 项目分布 Top 5，每项含 project_name、session_count、tokens */
    private List<Map<String, Object>> topProjects;

    /** 用户排行，每项含 user_id、session_count、tokens */
    private List<Map<String, Object>> topUsers;

    /** 能力热力图，每项含 capability_name、call_count */
    private List<Map<String, Object>> capabilityHeatmap;
}
