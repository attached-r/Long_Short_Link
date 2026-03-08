package rj.highlink.testService;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 压测配置类
 * 用于配置压测参数，方便调整测试规模
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "loadtest")
public class LoadTestProperties {

    /**
     * 小流量测试请求数
     */
    private int smallLoadRequests = 100;

    /**
     * 中流量测试请求数
     */
    private int mediumLoadRequests = 500;

    /**
     * 大流量测试请求数
     */
    private int highLoadRequests = 1000;

    /**
     * 超大流量测试请求数
     */
    private int veryHighLoadRequests = 2000;

    /**
     * 重定向小流量测试请求数
     */
    private int redirectSmallLoadRequests = 500;

    /**
     * 重定向中流量测试请求数
     */
    private int redirectMediumLoadRequests = 1000;

    /**
     * 重定向大流量测试请求数
     */
    private int redirectHighLoadRequests = 3000;

    /**
     * 线程池大小
     */
    private int threadPoolSize = 50;

    /**
     * 请求超时时间（秒）
     */
    private int timeoutSeconds = 60;

    /**
     * 限流测试请求数
     */
    private int rateLimitTestRequests = 30;

    /**
     * 混合场景 - 创建请求数
     */
    private int mixedCreateCount = 100;

    /**
     * 混合场景 - 查询请求数
     */
    private int mixedQueryCount = 300;

    /**
     * 混合场景 - 重定向请求数
     */
    private int mixedRedirectCount = 500;

    /**
     * 是否验证唯一性
     */
    private boolean verifyUniqueness = true;

    /**
     * 是否生成详细报告
     */
    private boolean generateReport = true;
}
