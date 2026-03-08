package rj.highlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import rj.highlink.entity.po.ShortLinkVisitPo;

import java.time.LocalDateTime;

/**
 * 短链接访问记录 Mapper 接口
 */

@Mapper
public interface ShortLinkVisitMapper extends BaseMapper<ShortLinkVisitPo> {
    /**
     * 一：统计指定短链的IP数 (UV)
     *
     * @param shortCode 短链码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return UV
     */
    Long countDistinctIp(
            @Param("shortCode") String shortCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
