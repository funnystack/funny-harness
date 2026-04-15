package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 月报看板 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class MonthlyVO {

    /** 安装覆盖率（安装用户数/团队总人数） */
    private BigDecimal coverageRate;

    /** Token 成本分析，含 totalTokens、avgPerUser、avgPerSession、costInYuan */
    private Map<String, Object> tokenCost;

    /** 项目×用户热度矩阵，每项含 project_name、user_id、session_count */
    private List<Map<String, Object>> projectUserMatrix;

    /** 推广建议列表（来自 metrics_reports） */
    private List<String> suggestions;
}
