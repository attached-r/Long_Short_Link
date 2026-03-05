package rj.highlink.entity.vo;

import lombok.Data;

/**
 * 短链重定向
 * 用于封装短链接重定向的相关信息
 */
@Data
public class RedirectVO {
    /**
     * 重定向目标地址
     */
    private String targetUrl;

    /**
     * 重定向类型
     * 0-临时跳转 1-永久跳转
     */
    private Integer redirectType;
}
