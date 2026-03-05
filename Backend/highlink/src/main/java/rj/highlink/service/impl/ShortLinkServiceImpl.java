package rj.highlink.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import rj.highlink.entity.dto.CreateShortLinkDTO;
import rj.highlink.entity.po.ShortLinkPo;
import rj.highlink.mapper.ShortLinkMapper;
import rj.highlink.service.ShortLinkService;
import rj.highlink.utils.*;
import rj.highlink.common.util.UrlBlackListUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短链接服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkPo> implements ShortLinkService {

    private final RedissonClient redissonClient;
    private final RedisUtil redisUtil;

    private static final String LOCK_KEY_PREFIX = "lock:hash:";  // 锁的key前缀
    private static final String SHORT_LINK_DOMAIN = "http://short.url/"; // 短链接域名
    private static final long MIN_EXPIRE_SECONDS = 3600;   // 最小过期时间1h，单位：秒

    /**
     * 一：创建短链接
     *
     * @param dto 创建短链接的参数类
     * @return 创建成功的短链接
     * 整体逻辑：
     * 1.黑名单过滤
     * 2.计算 URL 的 MD5 hash（作为去重依据）
     * 3.用 hash 加分布式锁（防止并发重复创建）
     * 4.双重检查（Double-Check）数据库是否已存在相同 longUrl
     * 5.不存在 → 生成短码（雪花ID → Base62）
     * 6.短码冲突时简单重试一次（+随机数）
     * 7.入库 + 写 Redis + 布隆过滤器
     * 8.返回短链接
     */
    @Override
    public String create(CreateShortLinkDTO dto) {
        // 1. 参数校验：检查URL是否在黑名单中
        String longUrl = dto.getLongUrl();
        if (UrlBlackListUtil.isBlack(longUrl)) {
            log.warn("尝试为黑名单URL生成短链接 url={}", longUrl);
            throw new IllegalArgumentException("该 URL 在黑名单中，无法生成短链接");
        }

        // 2. 计算url_hash
        String urlHash = DigestUtil.md5Hex(longUrl);

        // 3. 获取分布式锁
        String lockKey = LOCK_KEY_PREFIX + urlHash;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(15, TimeUnit.SECONDS)) {
                try {
                    // 4. 双检锁：查询数据库是否已存在
                    ShortLinkPo existPo = this.getOne(  // 查询数据库
                            new LambdaQueryWrapper<ShortLinkPo>()
                                    .eq(ShortLinkPo::getUrlHash, urlHash)
                    );
                    log.info("查询数据库结果：{}", existPo);

                    // 5. 若存在，直接返回已有短码
                    if (existPo != null) {
                        return SHORT_LINK_DOMAIN + existPo.getShortCode();
                    }

                    // 6. 生成雪花ID和随机盐，生成短码（冲突重试一次）
                    long snowflakeId = IdGenerator.nextId();
                    String shortCode = generateShortCode(snowflakeId);
                    if (this.exists(new LambdaQueryWrapper<ShortLinkPo>().eq(ShortLinkPo::getShortCode, shortCode))) {
                        // 冲突，重试一次
                        shortCode = generateShortCode(snowflakeId + new Random().nextInt(1000));
                    }

                    // 7. 构建PO，设置过期时间，插入数据库
                    ShortLinkPo shortLinkPo = new ShortLinkPo();
                    shortLinkPo.setShortCode(shortCode);
                    shortLinkPo.setLongUrl(longUrl);
                    shortLinkPo.setUrlHash(urlHash);
                    shortLinkPo.setStatus(1); // 启用状态
                    shortLinkPo.setExpireTime(dto.getExpireTime() != null ? dto.getExpireTime() : LocalDateTime.now().plusWeeks(1)); // 默认过期时间 一周
                    shortLinkPo.setCreateTime(LocalDateTime.now());
                    shortLinkPo.setUpdateTime(LocalDateTime.now());

                    this.save(shortLinkPo);  // 使用mybatis-plus 插入数据库，不用写sql
                    log.info("插入数据库结果：{}", shortLinkPo);

                    // 8. 写入 Redis，布隆过滤器添加
                    long expireSeconds = calculateExpireSeconds(shortLinkPo.getExpireTime());
                    redisUtil.setLink(shortCode, longUrl, expireSeconds);
                    redisUtil.addToBloom(shortCode);

                    log.info("写入 Redis 结果：{}", shortCode);
                    // 9. 返回完整短链接URL
                    return SHORT_LINK_DOMAIN + shortCode;

                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("获取分布式锁失败，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("创建短链接时被中断", e);
        }
    }


    /**
     * 计算 Redis 存储的过期秒数
     *
     * @param expireTime 过期时间
     * @return 过期秒数，至少为 MIN_EXPIRE_SECONDS，null 或无效时间返回 -1（永不过期）
     */
    private long calculateExpireSeconds(LocalDateTime expireTime) {
        if (expireTime == null) {
            return -1L; // 永不过期
        }
        // 计算剩余秒数
        long seconds = Duration.between(LocalDateTime.now(), expireTime).toSeconds();

        // 如果过期时间已过或小于最小值，使用最小过期时间
        if (seconds <= MIN_EXPIRE_SECONDS) {
            return MIN_EXPIRE_SECONDS;
        }

        return seconds;
    }

    /**
     * 生成短码
     */
    private String generateShortCode(long id) {
        return Base62Util.encode(id,null);
    }
}
