package rj.highlink.entity.vo;

import lombok.Data;

/**
 * 统计数据视图对象
 */
@Data
public class StatsVO {

    private String shortCode;

    private Long pv;  // 访问量（Page View）

    private Long uv;  // 独立访客数（Unique Visitor）

    private String startTime;

    private String endTime;
}
