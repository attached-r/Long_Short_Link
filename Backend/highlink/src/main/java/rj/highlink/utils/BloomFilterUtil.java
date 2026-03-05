package rj.highlink.utils;

import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器工具类
 * 用于短链去重和防穿透
 */
@Component
public class BloomFilterUtil {

    @Resource
    private RBloomFilter<String> shortLinkBloomFilter;

    /**
     * 将元素添加到布隆过滤器
     *
     * @param element 要添加的元素（短码）
     * @return 操作结果
     */
    public boolean add(String element) {
        try {
            shortLinkBloomFilter.add(element);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断元素是否可能存在于布隆过滤器
     *
     * @param element 要检查的元素（短码）
     * @return true-可能存在，false-一定不存在
     */
    public boolean contains(String element) {
        try {
            return shortLinkBloomFilter.contains(element);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
