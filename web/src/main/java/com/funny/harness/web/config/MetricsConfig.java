package com.funny.harness.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 度量体系配置绑定
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "metrics")
public class MetricsConfig {

    /**
     * API Key 映射，key=角色(admin/viewer)，value=api key
     */
    private Map<String, String> apiKeys;

    /**
     * 告警阈值配置
     */
    private AlertConfig alert = new AlertConfig();

    /**
     * Token 估算单价（元/1K tokens）
     */
    private double tokenPricePerK = 0.03;

    /**
     * 人工成本估算（元/小时）
     */
    private double manualCostPerHour = 200;

    /**
     * 原始数据保留天数
     */
    private int rawRetentionDays = 90;

    @Getter
    @Setter
    public static class AlertConfig {
        /** Token 消耗超过同类型任务平均值的倍数 */
        private int tokenOveruseRatio = 5;
        /** 修正循环次数上限 */
        private int loopExceededThreshold = 3;
        /** DAU 下降比例阈值 */
        private double dauDropRatio = 0.5;
        /** 周失败率阈值 */
        private double failureRateThreshold = 0.15;
        /** Agent 无响应分钟数 */
        private int agentStuckMinutes = 5;
    }

    /**
     * 校验 API Key 是否有效，返回对应角色名，无效返回 null
     */
    public String validateApiKey(String apiKey) {
        if (apiKeys == null || apiKey == null || apiKey.isBlank()) {
            return null;
        }
        for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
            if (apiKey.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
