package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Agent 运营看板 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class AgentDashboardVO {

    /** 今日任务数 */
    private Integer todayTasks;

    /** 一次通过率（loop_count=1 占比） */
    private BigDecimal firstPassRate;

    /** 平均单任务 Token 数 */
    private Integer avgTokensPerTask;

    /** 活跃 Agent 数 */
    private Integer activeAgentCount;

    /** 30 天任务完成率趋势，每项含 date、completion_rate */
    private List<Map<String, Object>> completionTrend;

    /** Agent 利用率分布，每项含 agent_type、task_count */
    private List<Map<String, Object>> agentUtilization;

    /** 成本趋势（Token 折人民币），每项含 date、cost_yuan */
    private List<Map<String, Object>> costTrend;
}
