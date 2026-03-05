package rj.highlink.entity.vo;

import lombok.Data;
/**
 * 短链接视图对象
 * 用于向前端展示短链接的完整信息，包含短链码、完整短链接、原始长链接及相关状态信息
 */
@Data
public class ShortLinkVO {
    private String shortCode;  // 短链码，唯一的标识符

    private String fullShortUrl;  // 完整短链接

    private String longUrl;  // 原始长链接

    private String createTimeStr;  // 创建时间

    private String expireTimeStr;  // 失效时间

    private String statusDesc;  // 状态描述
}

