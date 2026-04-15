package com.funny.harness.web.controller;

import com.funny.harness.common.ApiResult;
import com.funny.harness.service.IMetricsDashboardService;
import com.funny.harness.service.vo.AgentDashboardVO;
import com.funny.harness.service.vo.AlertVO;
import com.funny.harness.service.vo.DailyVO;
import com.funny.harness.service.vo.MonthlyVO;
import com.funny.harness.service.vo.TraceDetailVO;
import com.funny.harness.service.vo.WeeklyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 度量 Dashboard 查询 API
 * 提供 6 个 GET 接口供前端看板调用
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsDashboardController {

    private final IMetricsDashboardService dashboardService;

    /**
     * 日报看板
     * GET /api/metrics/daily?date=2026-04-12
     */
    @GetMapping("/daily")
    public ApiResult<DailyVO> getDaily(@RequestParam(required = false) String date) {
        return ApiResult.succ(dashboardService.getDaily(date));
    }

    /**
     * 周报看板
     * GET /api/metrics/weekly?startDate=2026-04-06&endDate=2026-04-12
     */
    @GetMapping("/weekly")
    public ApiResult<WeeklyVO> getWeekly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResult.succ(dashboardService.getWeekly(startDate, endDate));
    }

    /**
     * 月报看板
     * GET /api/metrics/monthly?startDate=2026-04-01&endDate=2026-04-30
     */
    @GetMapping("/monthly")
    public ApiResult<MonthlyVO> getMonthly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResult.succ(dashboardService.getMonthly(startDate, endDate));
    }

    /**
     * Agent 运营看板
     * GET /api/metrics/agent/dashboard
     */
    @GetMapping("/agent/dashboard")
    public ApiResult<AgentDashboardVO> getAgentDashboard() {
        return ApiResult.succ(dashboardService.getAgentDashboard());
    }

    /**
     * Agent Trace 链路详情
     * GET /api/metrics/agent/trace/{traceId}
     */
    @GetMapping("/agent/trace/{traceId}")
    public ApiResult<TraceDetailVO> getTraceDetail(@PathVariable String traceId) {
        TraceDetailVO detail = dashboardService.getTraceDetail(traceId);
        if (detail == null) {
            return ApiResult.fail("Trace 不存在：" + traceId);
        }
        return ApiResult.succ(detail);
    }

    /**
     * 告警历史查询
     * GET /api/metrics/alerts?type=&severity=&startDate=&endDate=
     */
    @GetMapping("/alerts")
    public ApiResult<List<AlertVO>> getAlerts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResult.succ(dashboardService.getAlerts(type, severity, startDate, endDate));
    }
}
