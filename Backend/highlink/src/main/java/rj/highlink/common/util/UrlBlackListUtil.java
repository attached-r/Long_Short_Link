package rj.highlink.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * URL 格式校验
 * 链接黑名单工具类
 * 从配置文件加载黑名单域名和关键词
 */
@Slf4j
@Component
public class UrlBlackListUtil {

    /** 从配置文件注入黑名单域名（逗号分隔） */
    @Value("${url.blacklist.domains:evil.com,malicious-site.com,phishing.com}")
    private String blacklistDomainsStr;

    /** 从配置文件注入非法关键词（逗号分隔） */
    @Value("${url.blacklist.keywords:porn,gambling,fraud}")
    private String blacklistKeywordsStr;

    /** 线程安全的黑名单域名集合 */
    private static Set<String> blackDomains;

    /** 线程安全的黑名单关键词集合 */
    private static Set<String> blackKeywords;

    /**
     * 初始化方法：在 Spring 容器启动时加载黑名单
     */
    @PostConstruct
    public void init() {
        // 解析域名黑名单
        blackDomains = Arrays.stream(blacklistDomainsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // 解析关键词黑名单
        blackKeywords = Arrays.stream(blacklistKeywordsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        log.info("URL 黑名单加载完成 - 域名：{}个，关键词：{}个",
                blackDomains.size(), blackKeywords.size());

        if (log.isDebugEnabled()) {
            log.debug("黑名单域名：{}", blackDomains);
            log.debug("黑名单关键词：{}", blackKeywords);
        }
    }

    /**
     * 检查 URL 是否在黑名单中
     *
     * @param url 待检查的 URL
     * @return true-在黑名单中，false-安全
     */
    public static boolean isBlack(String url) {
        if (url == null || url.isBlank()) {
            return true; // 空 URL 也视为不安全 ，拦截
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            // 1. 如果 host 为 null，说明 URL 格式不正确，视为不安全
            if (host == null) {
                log.warn("URL 格式错误（缺少主机名），视为不安全：{}", url);
                return true;
            }
            // 2. 检查域名是否在黑名单中
            if (blackDomains != null && blackDomains.contains(host)) {
                log.debug("域名黑名单匹配：{}", host);
                return true;
            }

            // 3. 检查是否包含非法关键词
            String lowerUrl = url.toLowerCase();
            if (blackKeywords != null) {
                for (String keyword : blackKeywords) {
                    if (lowerUrl.contains(keyword)) {
                        log.debug("关键词黑名单匹配：{} in {}", keyword, url);
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            // URL 格式错误，视为不安全
            log.warn("URL 格式错误，视为不安全：{}", url);
            return true;
        }
    }

    /**
     * 动态添加域名到黑名单（运行时）
     *
     * @param domain 域名
     */
    public static void addDomainToBlacklist(String domain) {
        if (domain != null && !domain.isBlank()) {
            blackDomains.add(domain.trim().toLowerCase());
            log.info("已添加域名到黑名单：{}", domain);
        }
    }

    /**
     * 动态添加关键词到黑名单（运行时）
     *
     * @param keyword 关键词
     */
    public static void addKeywordToBlacklist(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            blackKeywords.add(keyword.trim().toLowerCase());
            log.info("已添加关键词到黑名单：{}", keyword);
        }
    }

    /**
     * 从黑名单中移除域名
     *
     * @param domain 域名
     */
    public static void removeDomainFromBlacklist(String domain) {
        if (domain != null && !domain.isBlank()) {
            blackDomains.remove(domain.trim().toLowerCase());
            log.info("已从黑名单移除域名：{}", domain);
        }
    }

    /**
     * 获取所有黑名单域名（只读）
     *
     * @return 域名集合
     */
    public static Set<String> getBlackDomains() {
        return blackDomains != null ? Collections.unmodifiableSet(blackDomains) : Collections.emptySet();
    }

    /**
     * 获取所有黑名单关键词（只读）
     *
     * @return 关键词集合
     */
    public static Set<String> getBlackKeywords() {
        return blackKeywords != null ? Collections.unmodifiableSet(blackKeywords) : Collections.emptySet();
    }
}
