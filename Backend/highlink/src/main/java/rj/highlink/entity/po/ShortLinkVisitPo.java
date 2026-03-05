package rj.highlink.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/*
 * 短链接访问记录实体类
 * 对应数据库表 short_link_visit，用于存储每次短链接被访问的详细记录
 */
@Data
@TableName("short_link_visit")
public class ShortLinkVisitPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shortCode;

    private String ip;  // 访问者IP

    private String userAgent;  // 访问者UserAgent

    private LocalDateTime visitTime;  // 访问时间
}