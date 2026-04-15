package com.funny.harness.web.controller;

import com.funny.harness.common.ApiResult;
import com.funny.harness.common.consts.MetricsConsts;
import com.funny.harness.dao.entity.MetricsRawEventDO;
import com.funny.harness.service.IMetricsCollectorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 度量数据收集 API
 * 接收客户端上报的度量事件，写入 raw 表
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsCollectorController {

    private final IMetricsCollectorService metricsCollectorService;

    /**
     * 批量数据收集接口
     * 接收 JSON 数组，格式校验后写入 metrics_raw_events
     */
    @PostMapping("/collect")
    public ApiResult<Integer> collect(
            @RequestBody List<Map<String, Object>> events,
            @RequestHeader(value = MetricsConsts.HEADER_CLIENT_ID, defaultValue = "") String clientId,
            @RequestHeader(value = MetricsConsts.HEADER_BATCH_ID, defaultValue = "") String batchId,
            HttpServletRequest request) {

        if (events == null || events.isEmpty()) {
            return ApiResult.succ(0);
        }

        // 从 HTTP 请求提取客户端 IP（X-Forwarded-For 优先）
        String clientIp = extractClientIp(request);

        int savedCount = metricsCollectorService.collectEvents(events, clientId, batchId, clientIp);
        log.info("度量数据收集完成，batchId={}, clientId={}, ip={}, 接收={}, 存储={}",
                batchId, clientId, clientIp, events.size(), savedCount);

        return ApiResult.succ(savedCount);
    }

    /**
     * 提取客户端真实 IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(MetricsConsts.HEADER_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能含多个 IP，取第一个
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
