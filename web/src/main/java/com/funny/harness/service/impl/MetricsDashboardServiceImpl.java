package com.funny.harness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.funny.harness.common.consts.MetricsConsts;
import com.funny.harness.dao.entity.MetricsAgentTraceDO;
import com.funny.harness.dao.entity.MetricsAlertDO;
import com.funny.harness.dao.entity.MetricsDailySummaryDO;
import com.funny.harness.dao.entity.MetricsRawEventDO;
import com.funny.harness.dao.entity.MetricsReportDO;
import com.funny.harness.dao.mapper.MetricsAgentTraceMapper;
import com.funny.harness.dao.mapper.MetricsAlertMapper;
import com.funny.harness.dao.mapper.MetricsDailySummaryMapper;
import com.funny.harness.dao.mapper.MetricsRawEventMapper;
import com.funny.harness.dao.mapper.MetricsReportMapper;
import com.funny.harness.dao.mapper.MetricsSessionMapper;
import com.funny.harness.dao.mapper.MetricsSkillUsageMapper;
import com.funny.harness.dao.mapper.MetricsUserMapper;
import com.funny.harness.service.IMetricsDashboardService;
import com.funny.harness.service.vo.AgentDashboardVO;
import com.funny.harness.service.vo.AlertVO;
import com.funny.harness.service.vo.DailyVO;
import com.funny.harness.service.vo.MonthlyVO;
import com.funny.harness.service.vo.TraceDetailVO;
import com.funny.harness.service.vo.WeeklyVO;
import com.funny.harness.web.config.MetricsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 度量 Dashboard 查询 Service 实现
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsDashboardServiceImpl implements IMetricsDashboardService {

    private final MetricsDailySummaryMapper dailySummaryMapper;
    private final MetricsSessionMapper sessionMapper;
    private final MetricsSkillUsageMapper skillUsageMapper;
    private final MetricsAgentTraceMapper agentTraceMapper;
    private final MetricsAlertMapper alertMapper;
    private final MetricsUserMapper userMapper;
    private final MetricsReportMapper reportMapper;
    private final MetricsRawEventMapper rawEventMapper;
    private final MetricsConfig metricsConfig;
    private final ObjectMapper objectMapper;

    @Override
    public DailyVO getDaily(String date) {
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        String queryStr = queryDate.toString();

        // 查当日 daily_summary
        MetricsDailySummaryDO summary = dailySummaryMapper.selectOne(
                new LambdaQueryWrapper<MetricsDailySummaryDO>()
                        .eq(MetricsDailySummaryDO::getSummaryDate, queryDate));

        DailyVO vo = new DailyVO();
        vo.setSummary(summary != null ? toSummaryMap(summary) : Map.of());

        // Token 小时趋势
        vo.setTokenHourlyTrend(sessionMapper.sumTokensByHour(queryStr));

        // 项目分布 Top 5
        vo.setTopProjects(sessionMapper.topByProject(queryStr, 5));

        // 用户排行 Top 10
        vo.setTopUsers(sessionMapper.topByUser(queryStr, 10));

        // 能力热力图
        vo.setCapabilityHeatmap(skillUsageMapper.capabilityHeatmap(queryStr, queryStr));

        return vo;
    }

    @Override
    public WeeklyVO getWeekly(String startDate, String endDate) {
        LocalDate weekEnd = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : LocalDate.now().minusDays(1);
        LocalDate weekStart = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : weekEnd.minusDays(6);
        String start = weekStart.toString();
        String end = weekEnd.toString();

        // 汇总 7 天 daily_summary
        List<MetricsDailySummaryDO> summaries = dailySummaryMapper.sumSummaryByDateRange(start, end);

        WeeklyVO vo = new WeeklyVO();
        vo.setSummary(aggregateSummaries(summaries));

        // DAU 趋势
        List<Map<String, Object>> dauTrend = new ArrayList<>();
        for (MetricsDailySummaryDO s : summaries) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", s.getSummaryDate().toString());
            item.put("dau", s.getDau());
            dauTrend.add(item);
        }
        vo.setDauTrend(dauTrend);

        // 能力调用趋势
        vo.setCapabilityTrend(skillUsageMapper.capabilityTrend(start, end));

        // 本周新增用户
        vo.setNewUsers(userMapper.newUsersByDateRange(start, end));

        return vo;
    }

    @Override
    public MonthlyVO getMonthly(String startDate, String endDate) {
        LocalDate monthEnd = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : LocalDate.now().minusDays(1);
        LocalDate monthStart = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : monthEnd.minusDays(29);
        String start = monthStart.toString();
        String end = monthEnd.toString();

        MonthlyVO vo = new MonthlyVO();

        // 安装覆盖率（简化：统计有活跃会话的用户数）
        long activeUsers = sessionMapper.topByUser(start, 9999).size();
        // 团队总人数取 users 表总数
        Long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<>());
        BigDecimal coverage = totalUsers > 0
                ? BigDecimal.valueOf(activeUsers).divide(BigDecimal.valueOf(totalUsers), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        vo.setCoverageRate(coverage);

        // Token 成本分析
        List<MetricsDailySummaryDO> summaries = dailySummaryMapper.sumSummaryByDateRange(start, end);
        Map<String, Object> tokenCost = new HashMap<>();
        long totalTokens = summaries.stream().mapToLong(s -> s.getTotalTokens() != null ? s.getTotalTokens() : 0).sum();
        int totalSessions = summaries.stream().mapToInt(s -> s.getTotalSessions() != null ? s.getTotalSessions() : 0).sum();
        double costYuan = totalTokens / 1000.0 * metricsConfig.getTokenPricePerK();
        tokenCost.put("totalTokens", totalTokens);
        tokenCost.put("avgPerUser", activeUsers > 0 ? totalTokens / activeUsers : 0);
        tokenCost.put("avgPerSession", totalSessions > 0 ? totalTokens / totalSessions : 0);
        tokenCost.put("costInYuan", BigDecimal.valueOf(costYuan).setScale(2, RoundingMode.HALF_UP));
        vo.setTokenCost(tokenCost);

        // 项目×用户热度矩阵
        vo.setProjectUserMatrix(userMapper.projectUserMatrix(start, end));

        // 推广建议（从月报中提取）
        vo.setSuggestions(extractSuggestions(MetricsConsts.REPORT_MONTHLY));

        return vo;
    }

    @Override
    public AgentDashboardVO getAgentDashboard() {
        String today = LocalDate.now().toString();
        LocalDate monthAgo = LocalDate.now().minusDays(30);
        String monthAgoStr = monthAgo.toString();

        AgentDashboardVO vo = new AgentDashboardVO();

        // 今日任务数
        Long todayCount = agentTraceMapper.selectCount(
                new LambdaQueryWrapper<MetricsAgentTraceDO>()
                        .apply("DATE(started_at) = {0}", today));
        vo.setTodayTasks(todayCount != null ? todayCount.intValue() : 0);

        // 一次通过率
        vo.setFirstPassRate(agentTraceMapper.firstPassRate(today, today));

        // 平均单任务 Token
        List<MetricsAgentTraceDO> todayTraces = agentTraceMapper.selectList(
                new LambdaQueryWrapper<MetricsAgentTraceDO>()
                        .apply("DATE(started_at) = {0}", today));
        double avgTokens = todayTraces.stream()
                .mapToInt(t -> t.getTotalTokens() != null ? t.getTotalTokens() : 0)
                .average().orElse(0);
        vo.setAvgTokensPerTask((int) avgTokens);

        // 活跃 Agent 数
        Long activeAgentCount = agentTraceMapper.selectCount(
                new LambdaQueryWrapper<MetricsAgentTraceDO>()
                        .eq(MetricsAgentTraceDO::getStatus, MetricsConsts.TRACE_RUNNING));
        vo.setActiveAgentCount(activeAgentCount != null ? activeAgentCount.intValue() : 0);

        // 30 天完成率趋势
        vo.setCompletionTrend(agentTraceMapper.completionRateTrend(monthAgoStr, today));

        // Agent 利用率分布
        vo.setAgentUtilization(agentTraceMapper.utilizationByAgentType(monthAgoStr, today));

        // 成本趋势：按日聚合 token × 单价
        List<Map<String, Object>> costTrend = new ArrayList<>();
        List<Map<String, Object>> completionData = agentTraceMapper.completionRateTrend(monthAgoStr, today);
        for (Map<String, Object> item : completionData) {
            Map<String, Object> costItem = new HashMap<>();
            costItem.put("date", item.get("date"));
            long dayTokens = agentTraceMapper.selectList(
                    new LambdaQueryWrapper<MetricsAgentTraceDO>()
                            .apply("DATE(started_at) = {0}", item.get("date")))
                    .stream()
                    .mapToLong(t -> t.getTotalTokens() != null ? t.getTotalTokens() : 0)
                    .sum();
            costItem.put("costYuan", BigDecimal.valueOf(dayTokens / 1000.0 * metricsConfig.getTokenPricePerK())
                    .setScale(2, RoundingMode.HALF_UP));
            costTrend.add(costItem);
        }
        vo.setCostTrend(costTrend);

        return vo;
    }

    @Override
    public TraceDetailVO getTraceDetail(String traceId) {
        MetricsAgentTraceDO trace = agentTraceMapper.selectOne(
                new LambdaQueryWrapper<MetricsAgentTraceDO>()
                        .eq(MetricsAgentTraceDO::getTraceId, traceId));
        if (trace == null) {
            return null;
        }

        TraceDetailVO vo = new TraceDetailVO();
        vo.setTraceId(trace.getTraceId());
        vo.setSessionId(trace.getSessionId());
        vo.setAgentType(trace.getAgentType());
        vo.setTaskType(trace.getTaskType());
        vo.setTaskDesc(trace.getTaskDesc());
        vo.setStatus(trace.getStatus());
        vo.setTotalTokens(trace.getTotalTokens());
        vo.setTotalDurationMs(trace.getTotalDurationMs());
        vo.setFilesChanged(trace.getFilesChanged());
        vo.setLoopCount(trace.getLoopCount());
        vo.setStartedAt(trace.getStartedAt());
        vo.setCompletedAt(trace.getCompletedAt());

        // 从 raw_events 解析 Step 链路
        vo.setSteps(parseStepsFromRawEvents(trace.getSessionId()));

        // 关联能力调用记录
        vo.setCapabilityUsages(extractCapabilityUsages(trace.getTraceId()));

        return vo;
    }

    @Override
    public List<AlertVO> getAlerts(String type, String severity, String startDate, String endDate) {
        List<MetricsAlertDO> alerts = alertMapper.queryAlerts(type, severity, startDate, endDate);
        List<AlertVO> result = new ArrayList<>();
        for (MetricsAlertDO alert : alerts) {
            AlertVO vo = new AlertVO();
            vo.setId(alert.getId());
            vo.setAlertType(alert.getAlertType());
            vo.setSeverity(alert.getSeverity());
            vo.setTraceId(alert.getTraceId());
            vo.setDetails(alert.getDetails());
            vo.setNotified(alert.getNotified());
            vo.setCreatedStime(alert.getCreatedStime());
            result.add(vo);
        }
        return result;
    }

    // ========== 私有方法 ==========

    /**
     * 将 DailySummaryDO 转为 Map
     */
    private Map<String, Object> toSummaryMap(MetricsDailySummaryDO s) {
        Map<String, Object> map = new HashMap<>();
        map.put("summaryDate", s.getSummaryDate() != null ? s.getSummaryDate().toString() : null);
        map.put("dau", s.getDau());
        map.put("totalSessions", s.getTotalSessions());
        map.put("totalTokens", s.getTotalTokens());
        map.put("totalDurationSeconds", s.getTotalDurationSeconds());
        map.put("activeProjects", s.getActiveProjects());
        map.put("agentFirstPassRate", s.getAgentFirstPassRate());
        map.put("topSkill", s.getTopSkill());
        map.put("skillCallCount", s.getSkillCallCount());
        return map;
    }

    /**
     * 聚合多天 daily_summary 为周汇总
     */
    private Map<String, Object> aggregateSummaries(List<MetricsDailySummaryDO> summaries) {
        Map<String, Object> result = new HashMap<>();
        int totalDau = summaries.stream().mapToInt(s -> s.getDau() != null ? s.getDau() : 0).sum();
        long totalTokens = summaries.stream().mapToLong(s -> s.getTotalTokens() != null ? s.getTotalTokens() : 0).sum();
        int totalSessions = summaries.stream().mapToInt(s -> s.getTotalSessions() != null ? s.getTotalSessions() : 0).sum();
        long totalDuration = summaries.stream().mapToLong(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        int completedTasks = summaries.stream().mapToInt(s -> s.getAgentTasksCompleted() != null ? s.getAgentTasksCompleted() : 0).sum();
        int failedTasks = summaries.stream().mapToInt(s -> s.getAgentTasksFailed() != null ? s.getAgentTasksFailed() : 0).sum();

        result.put("days", summaries.size());
        result.put("totalDau", totalDau);
        result.put("totalTokens", totalTokens);
        result.put("totalSessions", totalSessions);
        result.put("totalDurationSeconds", totalDuration);
        result.put("agentTasksCompleted", completedTasks);
        result.put("agentTasksFailed", failedTasks);
        return result;
    }

    /**
     * 从 raw_events 解析 Step 链路（按时间排序）
     */
    private List<Map<String, Object>> parseStepsFromRawEvents(String sessionId) {
        List<Map<String, Object>> steps = new ArrayList<>();
        if (sessionId == null || sessionId.isBlank()) {
            return steps;
        }

        List<MetricsRawEventDO> rawEvents = rawEventMapper.selectList(
                new LambdaQueryWrapper<MetricsRawEventDO>()
                        .eq(MetricsRawEventDO::getProcessed, MetricsConsts.PROCESSED_DONE)
                        .apply("JSON_EXTRACT(raw_payload, '$.session_id') = {0}", sessionId)
                        .orderByAsc(MetricsRawEventDO::getReceivedAt));

        for (MetricsRawEventDO raw : rawEvents) {
            try {
                JsonNode payload = objectMapper.readTree(raw.getRawPayload());
                Map<String, Object> step = new HashMap<>();
                step.put("eventType", raw.getEventType());
                step.put("timestamp", payload.has("timestamp") ? payload.get("timestamp").asText() : null);
                // tool_use 事件提取工具信息
                if (MetricsConsts.EVENT_TOOL_USE.equals(raw.getEventType())) {
                    step.put("toolName", payload.has("tool_name") ? payload.get("tool_name").asText() : null);
                }
                // capability_use 事件提取能力信息
                if (MetricsConsts.EVENT_CAPABILITY_USE.equals(raw.getEventType())) {
                    step.put("capabilityType", payload.has("capability_type") ? payload.get("capability_type").asText() : null);
                    step.put("capabilityName", payload.has("capability_name") ? payload.get("capability_name").asText() : null);
                }
                steps.add(step);
            } catch (Exception e) {
                log.warn("解析 raw event 失败，id={}：{}", raw.getId(), e.getMessage());
            }
        }
        return steps;
    }

    /**
     * 提取关联能力调用记录
     */
    private List<Map<String, Object>> extractCapabilityUsages(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return List.of();
        }
        return skillUsageMapper.capabilityHeatmap(
                LocalDate.now().minusDays(30).toString(),
                LocalDate.now().toString());
    }

    /**
     * 从报告中提取推广建议
     */
    private List<String> extractSuggestions(String reportType) {
        List<String> suggestions = new ArrayList<>();
        MetricsReportDO report = reportMapper.selectOne(
                new LambdaQueryWrapper<MetricsReportDO>()
                        .eq(MetricsReportDO::getReportType, reportType)
                        .orderByDesc(MetricsReportDO::getReportDate)
                        .last("LIMIT 1"));

        if (report != null && report.getContentJson() != null) {
            try {
                JsonNode json = objectMapper.readTree(report.getContentJson());
                JsonNode sugNode = json.get("suggestions");
                if (sugNode != null && sugNode.isArray()) {
                    for (JsonNode item : sugNode) {
                        suggestions.add(item.asText());
                    }
                }
            } catch (Exception e) {
                log.warn("解析报告建议失败：{}", e.getMessage());
            }
        }
        return suggestions;
    }
}
