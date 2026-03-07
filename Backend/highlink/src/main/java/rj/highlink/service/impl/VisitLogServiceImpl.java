package rj.highlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rj.highlink.entity.po.ShortLinkPo;
import rj.highlink.entity.po.ShortLinkVisitPo;
import rj.highlink.mapper.ShortLinkVisitMapper;
import rj.highlink.service.VisitLogService;

import java.time.LocalDateTime;

/**
 * 访问日志服务实现类
 * 使用异步线程池批量写入数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitLogServiceImpl implements VisitLogService {
    // 访问日志Mapper
    private final ShortLinkVisitMapper shortLinkVisitMapper;

    /**
     * 异步保存访问日志
     * 使用 @Async 注解实现异步写入，不阻塞主线程
     */
    @Async("asyncLogExecutor")
    @Override
    public void saveVisitLog(String shortCode, String ip, String userAgent) {
        try {
            // 1. 参数校验
            if (shortCode == null || shortCode.isBlank()) {
                log.warn("访问日志参数错误 - shortCode 为空");
                return;
            }

            // 2. 构建访问记录对象
            ShortLinkVisitPo visitPo = new ShortLinkVisitPo();
            visitPo.setShortCode(shortCode);
            visitPo.setIp(ip != null ? ip : "unknown");
            visitPo.setUserAgent(userAgent != null ? userAgent : "unknown");
            visitPo.setVisitTime(LocalDateTime.now());

            // 3. 插入数据库
            shortLinkVisitMapper.insert(visitPo);

            log.debug("访问日志异步保存成功 - shortCode: {}, IP: {}", shortCode, ip);

        } catch (Exception e) {
            // 4. 异常处理：记录日志但不影响主流程
            log.error("访问日志保存失败 - shortCode: {}, 错误：{}", shortCode, e.getMessage(), e);
        }
    }
}
