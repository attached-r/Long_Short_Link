package rj.highlink.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花算法生成ID
 */

public class IdGenerator {
    /**
     * 雪花算法实例
     * 参数 (1, 1) 分别表示数据中心 ID 和工作机器 ID，适用于单机部署
     */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    /**
     * 生成下一个全局唯一 ID
     *
     * @return 64 位长整型 ID，趋势递增
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
