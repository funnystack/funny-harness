package com.funny.harness.web.controller;

import com.funny.harness.common.ApiResult;
import com.funny.harness.service.IMetricsDashboardService;
import com.funny.harness.service.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Dashboard API 6 个接口测试
 *
 * @author funny2048
 * @since 2026-04-11
 */
@ExtendWith(MockitoExtension.class)
class MetricsDashboardControllerTest {

    @Mock
    private IMetricsDashboardService dashboardService;

    private MetricsDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new MetricsDashboardController(dashboardService);
    }

    @Test
    @DisplayName("GET /daily 返回日报数据")
    void getDaily_returnsData() {
        DailyVO vo = new DailyVO();
        vo.setSummary(Map.of("dau", 10));
        when(dashboardService.getDaily("2026-04-12")).thenReturn(vo);

        ApiResult<DailyVO> result = controller.getDaily("2026-04-12");
        assertEquals(0, result.getCode());
        assertEquals(10, result.getData().getSummary().get("dau"));
    }

    @Test
    @DisplayName("GET /weekly 返回周报数据")
    void getWeekly_returnsData() {
        WeeklyVO vo = new WeeklyVO();
        vo.setSummary(Map.of("totalDau", 50));
        when(dashboardService.getWeekly("2026-04-06", "2026-04-12")).thenReturn(vo);

        ApiResult<WeeklyVO> result = controller.getWeekly("2026-04-06", "2026-04-12");
        assertEquals(50, result.getData().getSummary().get("totalDau"));
    }

    @Test
    @DisplayName("GET /monthly 返回月报数据")
    void getMonthly_returnsData() {
        MonthlyVO vo = new MonthlyVO();
        vo.setCoverageRate(BigDecimal.valueOf(75.5));
        when(dashboardService.getMonthly("2026-04-01", "2026-04-30")).thenReturn(vo);

        ApiResult<MonthlyVO> result = controller.getMonthly("2026-04-01", "2026-04-30");
        assertEquals(0, BigDecimal.valueOf(75.5).compareTo(result.getData().getCoverageRate()));
    }

    @Test
    @DisplayName("GET /agent/dashboard 返回 Agent 运营数据")
    void getAgentDashboard_returnsData() {
        AgentDashboardVO vo = new AgentDashboardVO();
        vo.setTodayTasks(15);
        vo.setFirstPassRate(BigDecimal.valueOf(80));
        when(dashboardService.getAgentDashboard()).thenReturn(vo);

        ApiResult<AgentDashboardVO> result = controller.getAgentDashboard();
        assertEquals(15, result.getData().getTodayTasks());
    }

    @Test
    @DisplayName("GET /agent/trace/{id} 存在时返回详情")
    void getTraceDetail_exists_returnsDetail() {
        TraceDetailVO vo = new TraceDetailVO();
        vo.setTraceId("T-123");
        vo.setStatus("completed");
        when(dashboardService.getTraceDetail("T-123")).thenReturn(vo);

        ApiResult<TraceDetailVO> result = controller.getTraceDetail("T-123");
        assertEquals("T-123", result.getData().getTraceId());
    }

    @Test
    @DisplayName("GET /agent/trace/{id} 不存在时返回失败")
    void getTraceDetail_notExists_returnsFail() {
        when(dashboardService.getTraceDetail("NOT-EXIST")).thenReturn(null);

        ApiResult<TraceDetailVO> result = controller.getTraceDetail("NOT-EXIST");
        assertEquals(-1, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("GET /alerts 返回告警列表")
    void getAlerts_returnsList() {
        AlertVO alert = new AlertVO();
        alert.setId(1L);
        alert.setAlertType("LOOP_EXCEEDED");
        when(dashboardService.getAlerts(null, null, null, null)).thenReturn(List.of(alert));

        ApiResult<List<AlertVO>> result = controller.getAlerts(null, null, null, null);
        assertEquals(1, result.getData().size());
        assertEquals("LOOP_EXCEEDED", result.getData().get(0).getAlertType());
    }

    @Test
    @DisplayName("GET /alerts 支持参数过滤")
    void getAlerts_withFilters() {
        when(dashboardService.getAlerts("DAU_DROP", "critical", "2026-04-01", "2026-04-11"))
                .thenReturn(List.of());

        ApiResult<List<AlertVO>> result = controller.getAlerts("DAU_DROP", "critical", "2026-04-01", "2026-04-11");
        assertTrue(result.getData().isEmpty());
        verify(dashboardService).getAlerts("DAU_DROP", "critical", "2026-04-01", "2026-04-11");
    }
}
