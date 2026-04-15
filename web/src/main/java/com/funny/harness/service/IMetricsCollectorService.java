package com.funny.harness.service;

import java.util.List;
import java.util.Map;

/**
 * 度量数据收集 Service
 *
 * @author funny2048
 * @since 2026-04-11
 */
public interface IMetricsCollectorService {

    /**
     * 批量收集事件，校验后写入 raw 表
     *
     * @param events   原始事件列表
     * @param clientId 客户端标识
     * @param batchId  批次 ID
     * @param clientIp 客户端 IP
     * @return 成功存储的记录数
     */
    int collectEvents(List<Map<String, Object>> events, String clientId, String batchId, String clientIp);

    /**
     * 解析 raw 表中未处理的事件，拆分写入 domain 表
     *
     * @return 本次处理的记录数
     */
    int processRawEvents();
}
