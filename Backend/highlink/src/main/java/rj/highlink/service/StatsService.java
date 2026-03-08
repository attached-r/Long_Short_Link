package rj.highlink.service;

import rj.highlink.entity.dto.StatsQueryDTO;
import rj.highlink.entity.vo.StatsVO;

/**
 * 统计服务接口
 */
public interface StatsService {

    /**
     * 查询统计数据（PV/UV）
     *
     * @param dto 查询参数
     * @return 统计数据
     */
    StatsVO getStats(StatsQueryDTO dto);

    /**
     * 记录访问统计（每次访问短链时调用）
     *
     * @param shortCode 短链码
     * @param ip 访问者 IP
     */
    void recordVisit(String shortCode, String ip);
}
