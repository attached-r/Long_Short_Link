package rj.highlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import rj.highlink.entity.dto.StatsQueryDTO;
import rj.highlink.entity.po.ShortLinkVisitPo;
import rj.highlink.entity.vo.StatsVO;
import rj.highlink.mapper.ShortLinkVisitMapper;
import rj.highlink.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    @Resource //短码访问统计
    private ShortLinkVisitMapper shortLinkVisitMapper;

    @Resource // Redis 客户端
    private RedisTemplate<String, Object> redisTemplate;

    private static final String STATS_PV_PREFIX = "stats:pv:"; // PV前缀
    private static final String STATS_UV_PREFIX = "stats:uv:";  // UV前缀
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 时间格式

    /**
     * 记录访问统计（每次访问短链时调用）
     * 使用 Redis 高性能存储：
     * - PV：使用 String 累加器，每次访问 +1
     * - UV：使用 HyperLogLog 去重统计独立访客
     * 数据保留 7 天自动过期
     *
     * @param shortCode 短链码
     * @param ip 访问者 IP 地址
     */
    @Override
    public void recordVisit(String shortCode, String ip) {
        try {
            // 构建 Redis 键
            String pvKey = STATS_PV_PREFIX + shortCode;
            String uvKey = STATS_UV_PREFIX + shortCode;

            // 记录 PV 和 UV 数据
            redisTemplate.opsForValue().increment(pvKey);
            redisTemplate.opsForHyperLogLog().add(uvKey, ip);// uv采用HyperLogLog二进制编码

            // 设置 7 天过期时间
            redisTemplate.expire(pvKey, 7, TimeUnit.DAYS);
            redisTemplate.expire(uvKey, 7, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("记录访问统计失败：{}", shortCode, e);
        }
    }



    /**
     * 查询统计数据（PV/UV）
     * 采用混合查询策略：
     * - 无时间范围：优先从 Redis 查询（高性能），失败则降级到数据库
     * - 有时间范围：直接从数据库查询（精确统计）
     *
     * @param dto 查询参数，包含短链码、开始时间、结束时间
     * @return 统计数据视图对象，包含 PV、UV 及时间范围信息
     */
    @Override
    public StatsVO getStats(StatsQueryDTO dto) {
        // 构建统计数据视图对象
        StatsVO vo = new StatsVO();

        // 设置基本参数
        vo.setShortCode(dto.getShortCode());

        // 格式化并设置时间范围
        if (dto.getStartTime() != null) {
            vo.setStartTime(dto.getStartTime().format(FORMATTER));
        }
        if (dto.getEndTime() != null) {
            vo.setEndTime(dto.getEndTime().format(FORMATTER));
        }

        Long pv;
        Long uv;

        // 根据是否有时间范围选择查询策略
        if (dto.getStartTime() == null && dto.getEndTime() == null) {
            // 无时间范围：优先查询 Redis（高性能）
            pv = getPvFromRedis(dto.getShortCode());
            uv = getUvFromRedis(dto.getShortCode());

            // Redis 无数据：降级查询数据库
            if (pv == null || uv == null) {
                pv = getPvFromDb(dto);
                uv = getUvFromDb(dto);
            }
        } else {
            // 有时间范围：直接查询数据库（精确统计）
            pv = getPvFromDb(dto);
            uv = getUvFromDb(dto);
        }

        // 设置最终统计数据（空值转为 0）
        vo.setPv(pv != null ? pv : 0L);
        vo.setUv(uv != null ? uv : 0L);

        return vo;
    }

    /**
     * 从 Redis 中获取 PV :访问量（Page View）
     *
     * @param shortCode 短码
     * @return PV
     */
    private Long getPvFromRedis(String shortCode) {
        try {
            String key = STATS_PV_PREFIX + shortCode;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Long.valueOf(value.toString());
            }
        } catch (Exception e) {
            log.error("从 Redis 获取 PV 失败：{}", shortCode, e);
        }
        return null;
    }

    /**
     * 从 Redis 中获取 UV:独立访客数（Unique Visitor）
     *
     * @param shortCode 短码
     * @return  UV
     */
    private Long getUvFromRedis(String shortCode) {
        try {
            String key = STATS_UV_PREFIX + shortCode;
            return redisTemplate.opsForHyperLogLog().size(key);
        } catch (Exception e) {
            log.error("从 Redis 获取 UV 失败：{}", shortCode, e);
        }
        return null;
    }

    /**
     * 从数据库MySql中获取 PV，访问量
     *
     * @param dto 查询参数
     * @return PV
     */
    private Long getPvFromDb(StatsQueryDTO dto) {
        // 1.创建查询条件
        LambdaQueryWrapper<ShortLinkVisitPo> wrapper = new LambdaQueryWrapper<>();
        // 2.设置查询条件，短码
        wrapper.eq(ShortLinkVisitPo::getShortCode, dto.getShortCode());
        // 3.设置查询条件，时间
        if (dto.getStartTime() != null) {
            // ge，大于等于
            wrapper.ge(ShortLinkVisitPo::getVisitTime, dto.getStartTime());
        }
        // 4.设置查询条件，时间
        if (dto.getEndTime() != null) {
            // le，小于等于
            wrapper.le(ShortLinkVisitPo::getVisitTime, dto.getEndTime());
        }

        return shortLinkVisitMapper.selectCount(wrapper);
    }

    /**
     * 从数据库MySql中获取 UV，独立访客数
     *
     * @param dto 访问统计参数
     * @return UV
     */
    private Long getUvFromDb(StatsQueryDTO dto) {
        // 这里用自定义mapper方法
        return shortLinkVisitMapper.countDistinctIp(dto.getShortCode(),
                dto.getStartTime(), dto.getEndTime());
    }
}
