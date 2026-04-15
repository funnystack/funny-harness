package com.funny.harness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.funny.harness.common.consts.MetricsConsts;
import com.funny.harness.dao.entity.MetricsAgentTraceDO;
import com.funny.harness.dao.entity.MetricsRawEventDO;
import com.funny.harness.dao.entity.MetricsSessionDO;
import com.funny.harness.dao.entity.MetricsSkillUsageDO;
import com.funny.harness.dao.entity.MetricsUserDO;
import com.funny.harness.dao.mapper.MetricsAgentTraceMapper;
import com.funny.harness.dao.mapper.MetricsRawEventMapper;
import com.funny.harness.dao.mapper.MetricsSessionMapper;
import com.funny.harness.dao.mapper.MetricsSkillUsageMapper;
import com.funny.harness.dao.mapper.MetricsUserMapper;
import com.funny.harness.service.IMetricsCollectorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 度量数据收集 Service 实现
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorServiceImpl implements IMetricsCollectorService {

    private final MetricsRawEventMapper rawEventMapper;
    private final MetricsUserMapper userMapper;
    private final MetricsSessionMapper sessionMapper;
    private final MetricsSkillUsageMapper skillUsageMapper;
    private final MetricsAgentTraceMapper agentTraceMapper;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 500;

    @Override
    public int collectEvents(List<Map<String, Object>> events, String clientId, String batchId, String clientIp) {
        int savedCount = 0;
        for (Map<String, Object> event : events) {
            // 基础校验：event_type 非空
            Object eventType = event.get("event_type");
            if (eventType == null || eventType.toString().isBlank()) {
                log.debug("跳过无效事件：缺少 event_type");
                continue;
            }

            // timestamp 校验
            Object timestamp = event.get("timestamp");
            if (timestamp == null || !isValidTimestamp(timestamp.toString())) {
                log.debug("跳过无效事件：timestamp 格式错误={}", timestamp);
                continue;
            }

            MetricsRawEventDO rawEvent = new MetricsRawEventDO();
            rawEvent.setBatchId(batchId.isBlank() ? null : batchId);
            rawEvent.setClientId(clientId.isBlank() ? "unknown" : clientId);
            rawEvent.setClientIp(clientIp);
            rawEvent.setEventType(eventType.toString());
            rawEvent.setProcessed(MetricsConsts.PROCESSED_PENDING);

            try {
                rawEvent.setRawPayload(objectMapper.writeValueAsString(event));
            } catch (Exception e) {
                log.warn("序列化事件失败：{}", e.getMessage());
                continue;
            }

            rawEvent.setReceivedAt(LocalDateTime.now());
            rawEventMapper.insert(rawEvent);
            savedCount++;
        }
        return savedCount;
    }

    @Override
    public int processRawEvents() {
        // 查询未处理的记录
        LambdaQueryWrapper<MetricsRawEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricsRawEventDO::getProcessed, MetricsConsts.PROCESSED_PENDING)
                .orderByAsc(MetricsRawEventDO::getReceivedAt)
                .last("LIMIT " + BATCH_SIZE);

        List<MetricsRawEventDO> rawEvents = rawEventMapper.selectList(wrapper);
        if (rawEvents.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (MetricsRawEventDO rawEvent : rawEvents) {
            try {
                JsonNode payload = objectMapper.readTree(rawEvent.getRawPayload());
                routeEvent(rawEvent.getEventType(), payload);
                markProcessed(rawEvent.getId(), MetricsConsts.PROCESSED_DONE, null);
                processedCount++;
            } catch (Exception e) {
                log.error("解析 raw event 失败，id={}：{}", rawEvent.getId(), e.getMessage());
                markProcessed(rawEvent.getId(), MetricsConsts.PROCESSED_FAILED, e.getMessage());
            }
        }

        log.info("原始事件解析完成，处理={} 条", processedCount);
        return processedCount;
    }

    /**
     * 按 event_type 路由到不同的处理逻辑
     */
    private void routeEvent(String eventType, JsonNode payload) {
        switch (eventType) {
            case MetricsConsts.EVENT_SESSION_START -> handleSessionStart(payload);
            case MetricsConsts.EVENT_TOOL_USE -> handleToolUse(payload);
            case MetricsConsts.EVENT_CAPABILITY_USE -> handleCapabilityUse(payload);
            case MetricsConsts.EVENT_SESSION_STOP -> handleSessionStop(payload);
            default -> log.warn("未知事件类型：{}", eventType);
        }
    }

    /**
     * 处理 session_start：创建会话记录 + 注册用户
     */
    private void handleSessionStart(JsonNode payload) {
        String userId = getTextOrEmpty(payload, "user_id");
        String sessionId = getTextOrEmpty(payload, "session_id");

        // 注册用户（不存在则插入）
        if (!userId.isBlank()) {
            ensureUserExists(payload, userId);
        }

        // 创建会话记录
        MetricsSessionDO session = new MetricsSessionDO();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setProjectName(getTextOrEmpty(payload, "project_name"));
        session.setStartedAt(parseTimestamp(getTextOrEmpty(payload, "timestamp")));
        sessionMapper.insert(session);
    }

    /**
     * 处理 tool_use：更新会话工具计数
     */
    private void handleToolUse(JsonNode payload) {
        String sessionId = getTextOrEmpty(payload, "session_id");
        String toolName = getTextOrEmpty(payload, "tool_name");

        if (sessionId.isBlank()) {
            return;
        }

        // 更新工具计数
        MetricsSessionDO session = sessionMapper.selectOne(
                new LambdaQueryWrapper<MetricsSessionDO>().eq(MetricsSessionDO::getSessionId, sessionId));
        if (session == null) {
            return;
        }

        String toolLower = toolName.toLowerCase();
        switch (toolLower) {
            case "read" -> session.setToolReadCount(session.getToolReadCount() + 1);
            case "edit" -> session.setToolEditCount(session.getToolEditCount() + 1);
            case "bash" -> session.setToolBashCount(session.getToolBashCount() + 1);
            case "grep" -> session.setToolGrepCount(session.getToolGrepCount() + 1);
        }
        sessionMapper.updateById(session);
    }

    /**
     * 处理 capability_use：写入能力调用记录
     */
    private void handleCapabilityUse(JsonNode payload) {
        MetricsSkillUsageDO usage = new MetricsSkillUsageDO();
        usage.setSessionId(getTextOrEmpty(payload, "session_id"));
        usage.setUserId(getTextOrEmpty(payload, "user_id"));
        usage.setProjectName(getTextOrEmpty(payload, "project_name"));
        usage.setCapabilityType(getTextOrEmpty(payload, "capability_type"));
        usage.setCapabilityName(getTextOrEmpty(payload, "capability_name"));
        usage.setTriggeredAt(parseTimestamp(getTextOrEmpty(payload, "timestamp")));
        usage.setResultStatus(MetricsConsts.RESULT_SUCCESS);
        skillUsageMapper.insert(usage);
    }

    /**
     * 处理 session_stop：更新会话结束信息 + 解析 task_summary
     */
    private void handleSessionStop(JsonNode payload) {
        String sessionId = getTextOrEmpty(payload, "session_id");

        if (sessionId.isBlank()) {
            return;
        }

        MetricsSessionDO session = sessionMapper.selectOne(
                new LambdaQueryWrapper<MetricsSessionDO>().eq(MetricsSessionDO::getSessionId, sessionId));
        if (session == null) {
            return;
        }

        // 更新结束信息
        session.setEndedAt(parseTimestamp(getTextOrEmpty(payload, "timestamp")));
        if (payload.has("duration_seconds")) {
            session.setDurationSeconds(payload.get("duration_seconds").asInt());
        }
        if (payload.has("total_tokens")) {
            session.setTotalTokens(payload.get("total_tokens").asInt());
        }
        sessionMapper.updateById(session);

        // 第二阶段：解析 task_summary，写入 agent_traces
        if (payload.has("task_summary")) {
            JsonNode summary = payload.get("task_summary");
            MetricsAgentTraceDO trace = new MetricsAgentTraceDO();
            trace.setTraceId("T-" + System.currentTimeMillis() + "-" + sessionId.hashCode());
            trace.setSessionId(sessionId);
            trace.setAgentType(getTextOrEmpty(summary, "agent_type"));
            trace.setTaskType(getTextOrEmpty(summary, "task_type"));
            trace.setTaskDesc(getTextOrEmpty(summary, "task_desc"));
            trace.setStatus(MetricsConsts.TRACE_COMPLETED);
            trace.setTotalTokens(summary.has("token_total") ? summary.get("token_total").asInt() : 0);
            trace.setFilesChanged(summary.has("files_changed") ? summary.get("files_changed").asInt() : 0);
            trace.setLoopCount(summary.has("loops") ? summary.get("loops").asInt() : 1);
            trace.setStartedAt(session.getStartedAt());
            trace.setCompletedAt(session.getEndedAt());
            agentTraceMapper.insert(trace);
        }
    }

    /**
     * 确保用户存在，不存在则创建
     */
    private void ensureUserExists(JsonNode payload, String userId) {
        MetricsUserDO existing = userMapper.selectOne(
                new LambdaQueryWrapper<MetricsUserDO>().eq(MetricsUserDO::getUserId, userId));
        if (existing != null) {
            return;
        }

        MetricsUserDO user = new MetricsUserDO();
        user.setUserId(userId);
        user.setUsername(getTextOrEmpty(payload, "username"));
        user.setHostname(getTextOrEmpty(payload, "hostname"));
        userMapper.insert(user);
    }

    /**
     * 标记 raw event 处理状态
     */
    private void markProcessed(Long id, int status, String error) {
        LambdaUpdateWrapper<MetricsRawEventDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MetricsRawEventDO::getId, id)
                .set(MetricsRawEventDO::getProcessed, status)
                .set(MetricsRawEventDO::getProcessError, error);
        rawEventMapper.update(null, wrapper);
    }

    private String getTextOrEmpty(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    private boolean isValidTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }
        try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return true;
        } catch (Exception e) {
            try {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
