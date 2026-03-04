package rj.highlink.utils;

/*
 * Base62 编码工具类
 * 用于将长整型 ID 转换为短码，支持添加随机盐防止遍历
 */
public class Base62Util {
    /** Base62 字符集：数字 + 小写字母 + 大写字母 */
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** 进制基数：62 */
    private static final int SCALE = 62;

    /**
     * 将分布式 ID 编码为 Base62 短码
     *
     * @param id 雪花算法生成的长整型 ID
     * @param salt 随机盐值，范围 0-999，可为 null。添加盐可防止短码被顺序遍历
     * @return 编码后的短码字符串
     *
     * @apiNote 编码逻辑：如果提供盐值，则将 ID 与盐混合 (id * 1000 + salt)，然后进行 Base62 转换
     */
    public static String encode(long id, Integer salt) {
        long num = salt == null ? id : id * 1000 + salt;
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            int idx = (int) (num % SCALE);  // 取余数
            sb.append(CHARS.charAt(idx));  // 获取对应字符
            num /= SCALE;
        }
        return sb.reverse().toString();
    }

    /**
     * 将 Base62 短码解码为原始数值
     *
     * @param shortCode Base62 编码的短码字符串
     * @return 解码后的长整型数值（如果编码时添加了盐，返回值为 id * 1000 + salt）
     *
     * @apiNote 该方法主要用于测试或特殊场景，实际业务中建议直接从数据库查询
     */
    public static long decode(String shortCode) {
        long num = 0;
        for (char c : shortCode.toCharArray()) {
            num = num * SCALE + CHARS.indexOf(c);
        }
        return num;
    }
}
