package com.funny.harness.service;

/**
 * 度量分析聚合 Service
 *
 * @author funny2048
 * @since 2026-04-11
 */
public interface IMetricsAnalysisService {

    /**
     * T1: 解析 raw 表中未处理的事件
     */
    void processRawEvents();

    /**
     * T2: 每日汇总
     */
    void dailySummary();

    /**
     * T3: 异常检测
     */
    void anomalyDetection();

    /**
     * T4: 周报生成
     */
    void weeklyReport();

    /**
     * T5: 月报生成
     */
    void monthlyReport();

    /**
     * T6: 原始数据清理
     */
    void cleanupRawEvents();
}
