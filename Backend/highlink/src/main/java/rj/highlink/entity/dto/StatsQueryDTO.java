package rj.highlink.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 统计数据查询参数
 */
@Data
public class StatsQueryDTO {

    private String shortCode; // 短链接码

    private LocalDateTime startTime;  // 开始时间

    private LocalDateTime endTime;  // 结束时间
}
