package rj.highlink.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import rj.highlink.common.result.R;
import rj.highlink.utils.RateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 限流拦截器
 * 基于 IP 和接口进行限流，防止恶意请求和系统过载
 * 用户请求
 *   ↓
 * 拦截器 preHandle()
 *   ↓
 * 获取客户端 IP + 请求 URI
 *   ↓
 * 判断接口类型 → 选择限流策略
 *   ├─ /shortlink/create → 10 次/分钟
 *   ├─ 重定向接口 → 100 次/分钟
 *   └─ 其他接口 → 放行
 *   ↓
 * 调用 Redis RateLimiter.tryAcquire()
 *   ↓
 * 检查是否超过限制
 *   ├─ 未超过 → 放行 (return true)
 *   └─ 超过 → 返回 429 (return false)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    // 注入限流工具类
    private final RateLimiter rateLimiter;
    // 注入JSON序列化工具
    private final ObjectMapper objectMapper;

    // 限流配置常量
    private static final int CREATE_SHORTLINK_LIMIT = 10;  // 创建短链接：10 次/分钟
    private static final int REDIRECT_LIMIT = 100;         // 重定向：100 次/分钟
    private static final int WINDOW_SECONDS = 60;          // 窗口时间：60 秒

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 根据接口类型设置限流 key 和阈值
        String rateLimitKey;
        int limit;

        if (uri.contains("/shortlink/create")) {
            // 创建短链接接口：严格限流（防止恶意创建）
            rateLimitKey = clientIp + ":create";
            limit = CREATE_SHORTLINK_LIMIT;
        } else if (uri.matches("/shortlink/[^/]+") || uri.matches("/[^/]+")) {
            // 重定向接口：宽松限流（正常访问频率较高）
            rateLimitKey = clientIp + ":redirect";
            limit = REDIRECT_LIMIT;
        } else {
            // 其他接口：默认放行
            return true;
        }

        // 执行限流检查
        boolean allowed = rateLimiter.tryAcquire(rateLimitKey, limit, WINDOW_SECONDS);

        if (!allowed) {
            log.warn("限流拦截 - IP: {}, 接口：{}, 限流 key: {}", clientIp, uri, rateLimitKey);
            handleRateLimitExceeded(response);
            return false;
        }

        log.debug("限流检查通过 - IP: {}, 接口：{}, 限流 key: {}", clientIp, uri, rateLimitKey);
        return true;
    }

    /**
     * 处理限流情况，返回标准JSON格式的429响应
     */
    private void handleRateLimitExceeded(HttpServletResponse response) {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json;charset=UTF-8");

        // 构建响应结果
        R<?> result = R.fail(429, "请求过于频繁，请稍后再试");

        // 使用ObjectMapper序列化JSON，避免直接toString()
        try (PrintWriter writer = response.getWriter()) {
            String json = objectMapper.writeValueAsString(result);
            writer.write(json);
            writer.flush();
        } catch (Exception e) {
            log.error("限流响应写入失败", e);
        }
    }

    /**
     * 获取客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 尝试从 X-Forwarded-For 获取（反向代理场景）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多个IP时取第一个（客户端真实IP）
            ip = ip.split(",")[0].trim();
            return ip;
        }

        // 尝试从 X-Real-IP 获取（Nginx等反向代理常用）
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 兜底：使用RemoteAddr（直接访问场景）
        ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }
}