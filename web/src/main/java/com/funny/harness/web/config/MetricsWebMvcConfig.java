package com.funny.harness.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 度量 WebMvc 配置
 * Dashboard 查询接口无需认证，暂不注册拦截器
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Configuration
public class MetricsWebMvcConfig implements WebMvcConfigurer {
}
