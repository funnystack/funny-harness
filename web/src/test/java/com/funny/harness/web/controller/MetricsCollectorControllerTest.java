package com.funny.harness.web.controller;

import com.funny.harness.common.ApiResult;
import com.funny.harness.service.IMetricsCollectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * POST /api/metrics/collect 接口测试
 *
 * @author funny2048
 * @since 2026-04-11
 */
@ExtendWith(MockitoExtension.class)
class MetricsCollectorControllerTest {

    @Mock
    private IMetricsCollectorService collectorService;

    private MetricsCollectorController controller;

    @BeforeEach
    void setUp() {
        controller = new MetricsCollectorController(collectorService);
    }

    @Test
    @DisplayName("空事件列表返回 0")
    void collect_emptyEvents_returnsZero() {
        ApiResult<Integer> result = controller.collect(List.of(), "client1", "batch1", new MockHttpServletRequest());
        assertNotNull(result);
        assertEquals(0, result.getData());
    }

    @Test
    @DisplayName("null 事件列表返回 0")
    void collect_nullEvents_returnsZero() {
        ApiResult<Integer> result = controller.collect(null, "client1", "batch1", new MockHttpServletRequest());
        assertNotNull(result);
        assertEquals(0, result.getData());
    }

    @Test
    @DisplayName("合法事件正常收集返回存储数量")
    void collect_validEvents_returnsCount() {
        when(collectorService.collectEvents(any(), anyString(), anyString(), anyString()))
                .thenReturn(2);

        List<Map<String, Object>> events = List.of(
                Map.of("event_type", "session_start", "timestamp", "2026-04-11T10:00:00Z"),
                Map.of("event_type", "tool_use", "timestamp", "2026-04-11T10:01:00Z")
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        ApiResult<Integer> result = controller.collect(events, "client1", "batch1", request);

        assertEquals(2, result.getData());
        verify(collectorService).collectEvents(eq(events), eq("client1"), eq("batch1"), anyString());
    }

    @Test
    @DisplayName("X-Forwarded-For 优先取 IP")
    void collect_forwardedFor_takesFirst() {
        when(collectorService.collectEvents(any(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        List<Map<String, Object>> events = List.of(
                Map.of("event_type", "session_start", "timestamp", "2026-04-11T10:00:00Z")
        );

        controller.collect(events, "c1", "b1", request);
        verify(collectorService).collectEvents(any(), anyString(), anyString(), eq("1.2.3.4"));
    }
}
