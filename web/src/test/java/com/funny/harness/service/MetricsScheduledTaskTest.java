package com.funny.harness.service;

import com.funny.harness.dao.mapper.*;
import com.funny.harness.service.impl.MetricsAnalysisServiceImpl;
import com.funny.harness.web.config.MetricsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 定时任务测试
 * 验证 6 个定时任务在无数据/有数据场景下不抛异常
 *
 * @author funny2048
 * @since 2026-04-11
 */
@ExtendWith(MockitoExtension.class)
class MetricsScheduledTaskTest {

    @Mock private IMetricsCollectorService collectorService;
    @Mock private MetricsRawEventMapper rawEventMapper;
    @Mock private MetricsSessionMapper sessionMapper;
    @Mock private MetricsSkillUsageMapper skillUsageMapper;
    @Mock private MetricsAgentTraceMapper agentTraceMapper;
    @Mock private MetricsDailySummaryMapper dailySummaryMapper;
    @Mock private MetricsAlertMapper alertMapper;
    @Mock private MetricsConfig metricsConfig;
    @Mock private ObjectMapper objectMapper;

    private MetricsAnalysisServiceImpl analysisService;

    @BeforeEach
    void setUp() {
        // 手动注入，因为 @RequiredArgsConstructor 需要全参数构造
        analysisService = new MetricsAnalysisServiceImpl(
                collectorService, rawEventMapper, sessionMapper, skillUsageMapper,
                agentTraceMapper, dailySummaryMapper, alertMapper, metricsConfig, objectMapper);
    }

    @Test
    @DisplayName("T1 原始事件解析：无数据返回 0")
    void t1_processRawEvents_noData() {
        when(collectorService.processRawEvents()).thenReturn(0);
        assertDoesNotThrow(() -> analysisService.processRawEvents());
    }

    @Test
    @DisplayName("T1 原始事件解析：有数据正常处理")
    void t1_processRawEvents_hasData() {
        when(collectorService.processRawEvents()).thenReturn(100);
        assertDoesNotThrow(() -> analysisService.processRawEvents());
    }

    @Test
    @DisplayName("T2 每日汇总：空数据不报错")
    void t2_dailySummary_noData() {
        assertDoesNotThrow(() -> analysisService.dailySummary());
    }

    @Test
    @DisplayName("T3 异常检测：空数据不报错")
    void t3_anomalyDetection_noData() {
        MetricsConfig.AlertConfig alertConfig = new MetricsConfig.AlertConfig();
        when(metricsConfig.getAlert()).thenReturn(alertConfig);
        assertDoesNotThrow(() -> analysisService.anomalyDetection());
    }

    @Test
    @DisplayName("T4 周报生成：正常执行")
    void t4_weeklyReport() {
        assertDoesNotThrow(() -> analysisService.weeklyReport());
    }

    @Test
    @DisplayName("T5 月报生成：正常执行")
    void t5_monthlyReport() {
        assertDoesNotThrow(() -> analysisService.monthlyReport());
    }

    @Test
    @DisplayName("T6 原始数据清理：无过期数据")
    void t6_cleanupRawEvents_noExpired() {
        when(metricsConfig.getRawRetentionDays()).thenReturn(90);
        when(rawEventMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> analysisService.cleanupRawEvents());
    }

    @Test
    @DisplayName("T6 原始数据清理：有过期数据正常删除")
    void t6_cleanupRawEvents_hasExpired() {
        when(metricsConfig.getRawRetentionDays()).thenReturn(90);
        when(rawEventMapper.selectCount(any())).thenReturn(50L);
        when(rawEventMapper.delete(any())).thenReturn(50);
        assertDoesNotThrow(() -> analysisService.cleanupRawEvents());
    }
}
