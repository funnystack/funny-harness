package com.funny.harness.common.consts;

/**
 * 度量体系常量
 *
 * @author funny2048
 * @since 2026-04-11
 */
public final class MetricsConsts {

    private MetricsConsts() {
    }

    // ========== 事件类型 ==========
    public static final String EVENT_SESSION_START = "session_start";
    public static final String EVENT_TOOL_USE = "tool_use";
    public static final String EVENT_CAPABILITY_USE = "capability_use";
    public static final String EVENT_SESSION_STOP = "session_stop";

    // ========== 能力类型 ==========
    public static final String CAP_TYPE_SKILL = "skill";
    public static final String CAP_TYPE_AGENT = "agent";
    public static final String CAP_TYPE_COMMAND = "command";
    public static final String CAP_TYPE_SLASH_COMMAND = "slash_command";

    // ========== 处理状态 ==========
    public static final int PROCESSED_PENDING = 0;
    public static final int PROCESSED_DONE = 1;
    public static final int PROCESSED_FAILED = 2;

    // ========== 告警类型 ==========
    public static final String ALERT_TOKEN_OVERUSE = "TOKEN_OVERUSE";
    public static final String ALERT_LOOP_EXCEEDED = "LOOP_EXCEEDED";
    public static final String ALERT_DAU_DROP = "DAU_DROP";
    public static final String ALERT_FAILURE_RATE = "FAILURE_RATE";
    public static final String ALERT_AGENT_STUCK = "AGENT_STUCK";

    // ========== 告警级别 ==========
    public static final String SEVERITY_INFO = "info";
    public static final String SEVERITY_WARNING = "warning";
    public static final String SEVERITY_CRITICAL = "critical";

    // ========== Trace 状态 ==========
    public static final String TRACE_RUNNING = "running";
    public static final String TRACE_COMPLETED = "completed";
    public static final String TRACE_FAILED = "failed";
    public static final String TRACE_TIMEOUT = "timeout";

    // ========== 报告类型 ==========
    public static final String REPORT_DAILY = "daily";
    public static final String REPORT_WEEKLY = "weekly";
    public static final String REPORT_MONTHLY = "monthly";

    // ========== 结果状态 ==========
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILED = "failed";
    public static final String RESULT_TIMEOUT = "timeout";

    // ========== API 角色 ==========
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_VIEWER = "viewer";

    // ========== HTTP Header ==========
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_BATCH_ID = "X-Batch-Id";
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
}
