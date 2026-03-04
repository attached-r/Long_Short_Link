package rj.highlink.common.util;

import java.net.URI;
import java.util.Set;

/*
 * URL 格式校验
 * 链接黑名单工具类
 */
public class UrlBlackListUtil {

    /** 非法域名黑名单 */
    private static final Set<String> BLACK_DOMAINS = Set.of(
            "evil.com",
            "malicious-site.com",
            "phishing.com"
    );

    /** 非法关键词黑名单 */
    private static final Set<String> BLACK_KEYWORDS = Set.of(
            "porn",
            "gambling",
            "fraud"
    );

    /**
     * 检查 URL 是否在黑名单中
     *
     * @param url 待检查的 URL
     * @return true-在黑名单中，false-安全
     */
    public static boolean isBlack(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            // 1. 检查域名是否在黑名单中
            if (BLACK_DOMAINS.contains(host)) {
                return true;
            }

            // 2. 检查是否包含非法关键词
            String lowerUrl = url.toLowerCase();
            for (String keyword : BLACK_KEYWORDS) {
                if (lowerUrl.contains(keyword)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // URL 格式错误，视为不安全
            return true;
        }
    }
}

