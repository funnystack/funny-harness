package com.funny.harness.service.impl;

import com.funny.harness.common.consts.MetricsConsts;
import com.funny.harness.dao.entity.MetricsAlertDO;
import com.funny.harness.dao.entity.MetricsAgentTraceDO;
import com.funny.harness.dao.entity.MetricsDailySummaryDO;
import com.funny.harness.dao.entity.MetricsRawEventDO;
import com.funny.harness.dao.mapper.MetricsAgentTraceMapper;
import com.funny.harness.dao.mapper.MetricsAlertMapper;
import com.funny.harness.dao.mapper.MetricsDailySummaryMapper;
import com.funny.harness.dao.mapper.MetricsRawEventMapper;
import com.funny.harness.dao.mapper.MetricsSessionMapper;
import com.funny.harness.dao.mapper.MetricsSkillUsageMapper;
import com.funny.harness.service.IMetricsAnalysisService;
import com.funny.harness.service.IMetricsCollectorService;
import com.funny.harness.web.config.MetricsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 度量分析聚合 Service 实现
 * 包含 6 个定时任务：事件解析、日汇总、异常检测、周报、月报、数据清理
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAnalysisServiceImpl implements IMetricsAnalysisService {

    private final IMetricsCollectorService collectorService;
    private final MetricsRawEventMapper rawEventMapper;
    private final MetricsSessionMapper sessionMapper;
    private final MetricsSkillUsageMapper skillUsageMapper;
    private final MetricsAgentTraceMapper agentTraceMapper;
    private final MetricsDailySummaryMapper dailySummaryMapper;
    private final MetricsAlertMapper alertMapper;
    private final MetricsConfig metricsConfig;
    private final ObjectMapper objectMapper;

    // ========== T1: 原始事件解析（每 1 分钟） ==========

    @Override
    @Scheduled(cron = "0 */1 * * * *")
    public void processRawEvents() {
        int count = collectorService.processRawEvents();
        if (count > 0) {
            log.info("T1 原始事件解析完成，处理 {} 条", count);
        }
    }

    // ========== T2: 每日汇总（每天 02:00） ==========

    @Override
    @Scheduled(cron = "0 2 * * * *")
    public void dailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.toString();

        log.info("T2 开始日汇总，日期={}", dateStr);

        // 聚合 sessions
        List<Map<String, Object>> sessionStats = sessionMapper.topByProject(dateStr, 9999);
        // 使用 SQL 聚合获取完整统计
        Map<String, Object> topUser = sessionMapper.topByUser(dateStr, 1).stream().findFirst().orElse(Map.of());

        // 聚合 skill_usage
        Map<String, Object> topSkill = skillUsageMapper.topSkillAndCount(dateStr, dateStr);

        // 聚合 agent_traces
        BigDecimal firstPassRate = agentTraceMapper.firstPassRate(dateStr, dateStr);
        List<Map<String, Object>> costByType = agentTraceMapper.costByTaskType(dateStr, dateStr);

        MetricsDailySummaryDO summary = new MetricsDailySummaryDO();
        summary.setSummaryDate(yesterday);
        summary.setAgentFirstPassRate(firstPassRate);

        if (topSkill != null) {
            summary.setTopSkill((String) topSkill.get("topSkill"));
            summary.setSkillCallCount(topSkill.get("skillCallCount") != null
                    ? ((Number) topSkill.get("skillCallCount")).intValue() : 0);
        }

        // 使用 INSERT ON DUPLICATE KEY UPDATE（通过先查后更新模拟）
        MetricsDailySummaryDO existing = dailySummaryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsDailySummaryDO>()
                        .eq(MetricsDailySummaryDO::getSummaryDate, yesterday));

        if (existing != null) {
            summary.setId(existing.getId());
            dailySummaryMapper.updateById(summary);
        } else {
            dailySummaryMapper.insert(summary);
        }

        log.info("T2 日汇总完成，日期={}", dateStr);
    }

    // ========== T3: 异常检测（每 5 分钟） ==========

    @Override
    @Scheduled(cron = "0 */5 * * * *")
    public void anomalyDetection() {
        String today = LocalDate.now().toString();

        // 规则1：修正循环过多
        checkLoopExceeded(today);

        // 规则2：DAU 突降
        checkDauDrop(today);

        log.debug("T3 异常检测完成");
    }

    private void checkLoopExceeded(String date) {
        int threshold = metricsConfig.getAlert().getLoopExceededThreshold();
        List<MetricsAgentTraceDO> traces = agentTraceMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsAgentTraceDO>()
                        .gt(MetricsAgentTraceDO::getLoopCount, threshold)
                        .apply("DATE(started_at) = {0}", date));

        for (MetricsAgentTraceDO trace : traces) {
            MetricsAlertDO alert = new MetricsAlertDO();
            alert.setAlertType(MetricsConsts.ALERT_LOOP_EXCEEDED);
            alert.setSeverity(MetricsConsts.SEVERITY_WARNING);
            alert.setTraceId(trace.getTraceId());
            alert.setDetails("{\"actual\":" + trace.getLoopCount() + ",\"threshold\":" + threshold + "}");
            alertMapper.insert(alert);
        }
    }

    private void checkDauDrop(String today) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        MetricsDailySummaryDO todaySummary = dailySummaryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsDailySummaryDO>()
                        .eq(MetricsDailySummaryDO::getSummaryDate, yesterday));
        MetricsDailySummaryDO yesterdaySummary = dailySummaryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsDailySummaryDO>()
                        .eq(MetricsDailySummaryDO::getSummaryDate, yesterday.minusDays(1)));

        if (todaySummary == null || yesterdaySummary == null) {
            return;
        }

        double ratio = (double) todaySummary.getDau() / yesterdaySummary.getDau();
        if (ratio < metricsConfig.getAlert().getDauDropRatio()) {
            MetricsAlertDO alert = new MetricsAlertDO();
            alert.setAlertType(MetricsConsts.ALERT_DAU_DROP);
            alert.setSeverity(MetricsConsts.SEVERITY_CRITICAL);
            alert.setDetails("{\"today\":" + todaySummary.getDau() + ",\"yesterday\":" + yesterdaySummary.getDau()
                    + ",\"ratio\":" + String.format("%.3f", ratio) + "}");
            alertMapper.insert(alert);
        }
    }

    // ========== T4: 周报生成（每周一 08:00） ==========

    @Override
    @Scheduled(cron = "0 0 8 ? * MON")
    public void weeklyReport() {
        LocalDate weekEnd = LocalDate.now().minusDays(1);
        LocalDate weekStart = weekEnd.minusDays(6);
        log.info("T4 开始生成周报，{} ~ {}", weekStart, weekEnd);
        // 周报详细逻辑后续迭代完善
        log.info("T4 周报生成完成");
    }

    // ========== T5: 月报生成（每月 1 日 08:00） ==========

    @Override
    @Scheduled(cron = "0 0 8 1 * ?")
    public void monthlyReport() {
        LocalDate monthEnd = LocalDate.now().minusDays(1);
        LocalDate monthStart = monthEnd.minusDays(29);
        log.info("T5 开始生成月报，{} ~ {}", monthStart, monthEnd);
        // 月报详细逻辑后续迭代完善
        log.info("T5 月报生成完成");
    }

    // ========== T6: 原始数据清理（每天 03:00） ==========

    @Override
    @Scheduled(cron = "0 3 * * * *")
    public void cleanupRawEvents() {
        int retentionDays = metricsConfig.getRawRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 查询需要清理的记录数
        Long count = rawEventMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsRawEventDO>()
                        .eq(MetricsRawEventDO::getProcessed, MetricsConsts.PROCESSED_DONE)
                        .lt(MetricsRawEventDO::getReceivedAt, cutoff));

        if (count > 0) {
            rawEventMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricsRawEventDO>()
                            .eq(MetricsRawEventDO::getProcessed, MetricsConsts.PROCESSED_DONE)
                            .lt(MetricsRawEventDO::getReceivedAt, cutoff));
            log.info("T6 原始数据清理完成，删除 {} 条（{} 天前）", count, retentionDays);
        }
    }
}
