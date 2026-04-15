package com.funny.harness.web.config;

import com.funny.harness.common.ApiResult;
import com.funny.harness.common.consts.MetricsConsts;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 度量 API Key 认证拦截器
 * 校验 X-API-Key 请求头，区分 Admin/Viewer 角色
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsAuthInterceptor implements HandlerInterceptor {

    private final MetricsConfig metricsConfig;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = request.getHeader(MetricsConsts.HEADER_API_KEY);
        String role = metricsConfig.validateApiKey(apiKey);

        if (role == null) {
            log.warn("度量 API 认证失败，IP={}，path={}", request.getRemoteAddr(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ApiResult<?> result = ApiResult.buildFailure(401, "无效的 API Key");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }

        // 将角色信息放入 request attribute，后续 Controller 可读取
        request.setAttribute("metricsRole", role);
        return true;
    }
}
