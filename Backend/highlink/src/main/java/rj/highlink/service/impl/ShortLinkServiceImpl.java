package rj.highlink.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import rj.highlink.entity.dto.CreateShortLinkDTO;
import rj.highlink.entity.po.ShortLinkPo;
import rj.highlink.entity.vo.ShortLinkVO;
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
@RequiredArgsConstructor  // 自动注入
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

        // 2. 计算url_hash (用于去重判断)
        String urlHash = DigestUtil.md5Hex(longUrl);

        // 3. 获取分布式锁 (基于urlHash，防止并发重复创建)
        String lockKey = LOCK_KEY_PREFIX + urlHash;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(15, TimeUnit.SECONDS)) {
                try {
                    // 4. 双检锁：查询数据库是否已存在相同的longUrl
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

    /**
     * 二：查询短链接详细信息
     * 1. 先从 Redis 缓存查询长链接（提高性能）
     * 2. 如果缓存命中，直接返回 VO（快速路径）
     * 3. 如果缓存未命中，则查询数据库，并回写缓存
     * 4. 构建 VO 对象返回
     *
     * @param shortCode 短链码
     * @return 短链接视图对象（不存在返回 null）
     */
    @Override
    public ShortLinkVO getInfo(String shortCode) {
        try {
            // 1. 先从 Redis 缓存查询长链接
            String longUrlFromCache = redisUtil.getLink(shortCode);

            // 2. 如果缓存命中，直接构建 VO 返回（快速路径）
            if (longUrlFromCache != null) {
                log.debug("缓存命中 - shortCode: {}, longUrl: {}", shortCode, longUrlFromCache);

                // 需要查询数据库获取完整信息（创建时间、过期时间、状态等）
                ShortLinkPo cachedPo = this.getOne(
                        new LambdaQueryWrapper<ShortLinkPo>()
                                .eq(ShortLinkPo::getShortCode, shortCode)
                );

                if (cachedPo != null) {
                    return buildShortLinkVO(cachedPo);
                }
            }

            // 3. 缓存未命中，查询数据库
            log.debug("缓存未命中，查询数据库 - shortCode: {}", shortCode);
            ShortLinkPo shortLinkPo = this.getOne(
                    new LambdaQueryWrapper<ShortLinkPo>()
                            .eq(ShortLinkPo::getShortCode, shortCode)
            );

            // 4. 如果不存在，返回 null
            if (shortLinkPo == null) {
                log.warn("查询短链接不存在 - shortCode: {}", shortCode);
                return null;
            }

            // 5. 构建 VO 对象
            ShortLinkVO vo = buildShortLinkVO(shortLinkPo);

            log.info("查询短链接成功 - shortCode: {}, longUrl: {}",
                    shortCode, shortLinkPo.getLongUrl());
            return vo;

        } catch (Exception e) {
            log.error("查询短链接异常 - shortCode: {}", shortCode, e);
            return null;
        }
    }

    /**
     * 构建 ShortLinkVO 对象的辅助方法
     *
     * @param po 数据库实体对象
     * @return 视图对象
     */
    private ShortLinkVO buildShortLinkVO(ShortLinkPo po) {
        ShortLinkVO vo = new ShortLinkVO();
        vo.setShortCode(po.getShortCode());
        vo.setFullShortUrl(SHORT_LINK_DOMAIN + po.getShortCode());
        vo.setLongUrl(po.getLongUrl());

        // 格式化时间
        if (po.getCreateTime() != null) {
            vo.setCreateTimeStr(po.getCreateTime().toString());
        }
        if (po.getExpireTime() != null) {
            vo.setExpireTimeStr(po.getExpireTime().toString());
        }

        // 设置状态描述
        vo.setStatusDesc(po.getStatus() == 1 ? "启用" : "禁用");

        return vo;
    }

    /**
     * 三：禁用短链接
     * 1. 更新数据库 status = 0（禁用状态）
     * 2. 删除 Redis 缓存（使用 deleteLink 方法）
     * 3. 确保后续访问会被拦截
     *
     * @param shortCode 短链码
     * @return 操作结果（true=成功，false=失败）
     */
    @Override
    public boolean disable(String shortCode) {
        try {
            // 1. 查询短链接是否存在
            ShortLinkPo shortLinkPo = this.getOne(
                    new LambdaQueryWrapper<ShortLinkPo>()
                            .eq(ShortLinkPo::getShortCode, shortCode)
            );

            if (shortLinkPo == null) {
                log.warn("禁用短链接失败 - 短链接不存在：{}", shortCode);
                return false;
            }

            // 2. 如果已经禁用，直接返回成功
            if (shortLinkPo.getStatus() == 0) {
                log.info("短链接已禁用 - shortCode: {}", shortCode);
                return true;
            }

            // 3. 更新数据库状态为禁用
            shortLinkPo.setStatus(0);
            shortLinkPo.setUpdateTime(LocalDateTime.now());
            this.updateById(shortLinkPo);

            // 4. 删除 Redis 缓存（逻辑删除，添加删除标记）
            redisUtil.deleteLink(shortCode);

            log.info("禁用短链接成功 - shortCode: {}, longUrl: {}",
                    shortCode, shortLinkPo.getLongUrl());
            return true;

        } catch (Exception e) {
            log.error("禁用短链接异常 - shortCode: {}", shortCode, e);
            return false;
        }
    }
}
