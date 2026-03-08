package rj.highlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rj.highlink.entity.po.ShortLinkPo;
import rj.highlink.entity.po.ShortLinkVisitPo;
import rj.highlink.mapper.ShortLinkMapper;
import rj.highlink.mapper.ShortLinkVisitMapper;
import rj.highlink.service.RedirectionService;
import rj.highlink.service.StatsService;
import rj.highlink.service.VisitLogService;
import rj.highlink.utils.BloomFilterUtil;
import rj.highlink.utils.RedisUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 一：短链重定向服务实现类
 * 具体流程逻辑：
 * 1.布隆过滤器 → 初步筛掉几乎肯定不存在的 shortCode（防穿透）
 * 2.黑名单（空值缓存） → 快速拒绝已确认不存在/失效的码
 * 3.Redis 正常缓存命中 → 直接返回 + 异步记日志（最常见快路径）
 * 4.未命中 → 加分布式锁
 * 5.双重检查 Redis（防锁等待期间其他线程已回写）
 * 6.查数据库
 * 7.根据状态/过期时间决定：
 * 8.无效 → 缓存黑名单（10分钟）
 * 9.有效 → 回写 Redis（使用数据库的真实剩余过期时间）
 *
 * 二：异步记录访问日志
 * 返回长链接 or null（404）
 * 每次记录日志时，需记录访问数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectionServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkPo> implements RedirectionService {

    private final RedissonClient redissonClient; // Redisson 客户端
    private final RedisUtil redisUtil;         // Redis 工具类
    private final BloomFilterUtil bloomFilterUtil;  // 布隆过滤器
    private final VisitLogService visitLogService;  // 访问日志服务
    private final HttpServletRequest request;  // 自动注入请求对象
    private final StatsService statsService;  // 统计服务

    private static final String LOCK_KEY_PREFIX = "lock:code:"; // 锁前缀

    @Override
    public String redirect(String shortCode) {
        log.info("开始处理短链重定向请求 - shortCode: {}, IP: {}", shortCode, getClientIp(request));

        // 0. 获取请求信息，防止 NPE
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "unknown";

        // 1. 布隆过滤器校验：不存在直接返回 null（Controller 处理 404）
        if (!bloomFilterUtil.contains(shortCode)) {
            log.warn("布隆过滤器拦截 - shortCode: {} (一定不存在)", shortCode);
            return null;
        }

        // 2. 检查空值缓存：命中直接返回 null
        if (redisUtil.isBlack(shortCode)) {
            log.debug("黑名单缓存拦截 - shortCode: {}", shortCode);
            return null;
        }

        // 3. 查询正常缓存：命中直接返回长链接
        String longUrl = redisUtil.getLink(shortCode);
        if (longUrl != null) {
            log.debug("Redis 缓存命中 - shortCode: {}, longUrl: {}", shortCode, longUrl);
            // 异步记录访问日志
            visitLogService.saveVisitLog(shortCode, clientIp, userAgent);
            // 记录统计数据,计数
            statsService.recordVisit(shortCode, clientIp);
            return longUrl;
        }

        log.debug("Redis 缓存未命中，准备查询数据库 - shortCode: {}", shortCode);

        // 4. 获取分布式锁，防止缓存击穿
        String lockKey = LOCK_KEY_PREFIX + shortCode;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {  // 尝试获取锁，最多等待 5 秒
                try {
                    log.trace("成功获取分布式锁 - shortCode: {}", shortCode);

                    // 5. 双检缓存：再次检查缓存，避免锁等待期间其他线程已加载数据
                    longUrl = redisUtil.getLink(shortCode);
                    if (longUrl != null) {
                        log.debug("双检缓存命中 - shortCode: {}", shortCode);
                        // 异步记录访问日志
                        visitLogService.saveVisitLog(shortCode, clientIp, userAgent);
                        // 统计访问数据,计数
                        statsService.recordVisit(shortCode, clientIp);
                        return longUrl;
                    }

                    // 6. 查询数据库
                    log.trace("开始查询数据库 - shortCode: {}", shortCode);
                    ShortLinkPo shortLinkPo = this.getOne(
                            new LambdaQueryWrapper<ShortLinkPo>()
                                    .eq(ShortLinkPo::getShortCode, shortCode) // 查询条件
                    );

                    if (shortLinkPo == null) {
                        log.warn("数据库中不存在该短链 - shortCode: {}", shortCode);
                        redisUtil.setBlack(shortCode, 60 * 10);
                        return null;
                    }

                    // 6.2 校验状态和过期时间
                    if (shortLinkPo.getStatus() == 0 || shortLinkPo.getExpireTime().isBefore(LocalDateTime.now())) {
                        log.warn("短链已禁用或已过期 - shortCode: {}, status: {}, expireTime: {}",
                                shortCode, shortLinkPo.getStatus(), shortLinkPo.getExpireTime());
                        redisUtil.setBlack(shortCode, 60 * 10);
                        return null;
                    }

                    // 7. 将长链接写入 Redis，设置与 DB 一致的过期时间
                    long expireSeconds = java.time.Duration.between(LocalDateTime.now(), shortLinkPo.getExpireTime()).getSeconds();
                    redisUtil.setLink(shortCode, shortLinkPo.getLongUrl(), expireSeconds);
                    log.debug("回写 Redis 缓存成功 - shortCode: {}, expireSeconds: {}", shortCode, expireSeconds);

                    // 8. 异步记录访问日志, 改为调用VisitLogService
                    visitLogService.saveVisitLog(shortCode, clientIp, userAgent);
                        // 统计访问数据,计数
                    statsService.recordVisit(shortCode, clientIp);

                    // 9. 返回长链接
                    log.info("重定向成功 - shortCode: {}, longUrl: {}", shortCode, shortLinkPo.getLongUrl());
                    return shortLinkPo.getLongUrl();

                } finally {
                    lock.unlock();
                    log.trace("释放分布式锁 - shortCode: {}", shortCode);
                }
            } else {
                log.error("获取分布式锁失败 - shortCode: {}", shortCode);
                throw new RuntimeException("获取分布式锁失败，请稍后重试");
            }
        } catch (InterruptedException e) {
            log.error("重定向过程被中断 - shortCode: {}", shortCode, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("重定向时被中断", e);
        } catch (Exception e) {
            log.error("重定向过程发生异常 - shortCode: {}", shortCode, e);
            throw e;
        }
    }


    /**
     * 获取客户端真实 IP 地址
     *
     * 按照优先级从 HTTP 请求头中提取 IP 地址，支持反向代理场景：
     * 1. X-Forwarded-For：经过代理时的客户端原始 IP（可能包含多个 IP，取第一个）
     * 2. X-Real-IP：Nginx 等代理服务器传递的真实 IP
     * 3. Proxy-Client-IP：Apache Httpd 代理的客户端 IP
     * 4. WL-Proxy-Client-IP：WebLogic 代理的客户端 IP
     * 5. RemoteAddr：直接连接的客户端 IP 或最后一个代理的 IP
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址，如果无法获取则返回 "unknown"
     */
    private String getClientIp(HttpServletRequest request) {
        // 1. 参数校验
        if (request == null) {
            return "unknown";
        }

        // 2. 尝试从 X-Forwarded-For 获取（支持多级代理）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 可能有多个代理，取第一个非内网的
            ip = ip.split(",")[0].trim();
            return ip;
        }

        // 3. 尝试从 X-Real-IP 获取（Nginx 常用）
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 4. 尝试从 Proxy-Client-IP 获取（Apache Httpd）
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 5. 尝试从 WL-Proxy-Client-IP 获取（WebLogic）
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 6. 直接使用远程地址作为兜底方案
        ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }
}

