package rj.highlink.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接实体类
 * PO : “数据库的镜像”，只和数据库交互，包含数据库的所有字段
 * 对应数据库表 short_link，用于存储短链接与长链接的映射关系及相关信息
 */
@Data
@TableName("short_link")  // Mybatis-Plus  可以绑定表名
public class ShortLinkPo {

    @TableId(type = IdType.AUTO)  // 绑定主键,并且自增
    private Long id;

    private String shortCode;  // 短链接

    private String longUrl;    // 长链接

    private String urlHash;   // URL的哈希值

    private Integer status;   //

    private LocalDateTime expireTime;  // 失效时间

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

/**
 * 短链接实体类
 * 对应数据库表 short_link，用于存储短链接与长链接的映射关系及相关信息
 */