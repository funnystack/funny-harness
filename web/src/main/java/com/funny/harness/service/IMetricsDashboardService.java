package com.funny.harness.service;

import com.funny.harness.service.vo.AgentDashboardVO;
import com.funny.harness.service.vo.AlertVO;
import com.funny.harness.service.vo.DailyVO;
import com.funny.harness.service.vo.MonthlyVO;
import com.funny.harness.service.vo.TraceDetailVO;
import com.funny.harness.service.vo.WeeklyVO;

import java.util.List;

/**
 * 度量 Dashboard 查询 Service
 *
 * @author funny2048
 * @since 2026-04-11
 */
public interface IMetricsDashboardService {

    /** 日报看板 */
    DailyVO getDaily(String date);

    /** 周报看板 */
    WeeklyVO getWeekly(String startDate, String endDate);

    /** 月报看板 */
    MonthlyVO getMonthly(String startDate, String endDate);

    /** Agent 运营看板 */
    AgentDashboardVO getAgentDashboard();

    /** Agent Trace 链路详情 */
    TraceDetailVO getTraceDetail(String traceId);

    /** 告警历史查询 */
    List<AlertVO> getAlerts(String type, String severity, String startDate, String endDate);
}
