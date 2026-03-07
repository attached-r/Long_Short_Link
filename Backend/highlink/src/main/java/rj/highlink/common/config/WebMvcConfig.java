package rj.highlink.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import rj.highlink.common.interceptor.RateLimitInterceptor;

/**
 * Web 配置类：
 * - 添加拦截器 RateLimitInterceptor
 * - 配置正确的拦截路径
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/shortlink/**",          // 短链接管理接口（创建/查询等）
                        "/{shortCode:[a-zA-Z0-9]+}") // 短码重定向接口（精确匹配字母数字）
                .excludePathPatterns(
                        "/shortlink/info/**",     // 查询详情放宽限流
                        "/actuator/**",           // 监控接口
                        "/health",                // 健康检查
                        "/static/**",             // 静态资源
                        "/public/**",             // 公共资源
                        "/favicon.ico",           // 网站图标
                        "/error"                  // 错误页面
                );
    }
}