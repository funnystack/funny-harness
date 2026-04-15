package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent Trace 链路详情 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class TraceDetailVO {

    /** 基本信息 */
    private String traceId;
    private String sessionId;
    private String agentType;
    private String taskType;
    private String taskDesc;
    private String status;
    private Integer totalTokens;
    private Integer totalDurationMs;
    private Integer filesChanged;
    private Integer loopCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /** Step 链路列表（从 raw_payload 解析），每个 Step 含 tool_name、duration_ms、tokens、status */
    private List<Map<String, Object>> steps;

    /** 关联能力调用记录 */
    private List<Map<String, Object>> capabilityUsages;
}
