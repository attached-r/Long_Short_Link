package rj.highlink.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 短链接每日统计数据实体类
 * 对应数据库表 short_link_stats_daily，用于存储短链接的每日访问统计数据
 */
@Data
@TableName("short_link_stats_daily")
public class ShortLinkStatsDailyPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shortCode;

    private LocalDate statsDate;  // 统计日期

    private Long pv;  // 页面访问量,短链接被访问总次数

    private Long uv;  // 用户访问量,短链接被访问不重复总人数

    private Long ipCount;  // 不重复的 IP访问量
}