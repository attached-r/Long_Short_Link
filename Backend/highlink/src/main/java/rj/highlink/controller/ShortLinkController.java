package rj.highlink.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rj.highlink.common.result.R;
import rj.highlink.entity.dto.CreateShortLinkDTO;
import rj.highlink.entity.vo.ShortLinkVO;
import rj.highlink.service.RedirectionService;
import rj.highlink.service.ShortLinkService;

import java.time.LocalDateTime;

/**
 * 短链接控制器
 * 提供短链接创建和重定向功能
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shortlink")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final RedirectionService redirectionService;

    /**
     * 一：创建短链接
     * POST /shortlink/create
     *
     * @param dto 创建短链接的请求参数
     * @return 包含短链接信息的响应
     */
    @PostMapping("/create")
    public R<ShortLinkVO> create(@Valid @RequestBody CreateShortLinkDTO dto) {
        try {
            // 1. 创建短链接
            String fullShortUrl = shortLinkService.create(dto);

            // 2. 构建响应数据

            ShortLinkVO vo = new ShortLinkVO();
            vo.setFullShortUrl(fullShortUrl);
            vo.setShortCode(extractShortCode(fullShortUrl));
            vo.setLongUrl(dto.getLongUrl());

            // 3. 设置时间信息
            LocalDateTime expireTime = dto.getExpireTime() != null
                    ? dto.getExpireTime():
                    LocalDateTime.now().plusWeeks(1);
            LocalDateTime createTime = LocalDateTime.now();

            if (createTime != null) {
                vo.setCreateTimeStr(createTime.toString());
            }
            if (expireTime != null) {
                vo.setExpireTimeStr(expireTime.toString());
            }

            return R.ok("短链接创建成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("创建短链接失败：{}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("创建短链接异常", e);
            return R.fail("创建短链接失败，请稍后重试");
        }
    }

    /**
     * 二：短链接重定向
     * GET /{shortCode}
     *
     * @param shortCode 短链码
     * @return 302 重定向响应或 404，直接访问短链接就可以访问长链接
     */
    @GetMapping("/redirect/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        try {
            // 1. 重定向 获取长链接
            String longUrl = redirectionService.redirect(shortCode);
            // 2. 判断长链接是否有效
            if (longUrl == null) {
                log.warn("重定向失败，短链接不存在或已失效：{}", shortCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            // 3. 构建重定向响应
            // 创建 HTTP 头
            HttpHeaders headers = new HttpHeaders();
            // 添加 Location 头，设置为长链接
            headers.add(HttpHeaders.LOCATION, longUrl);
            // 4. 返回重定向响应
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();

        } catch (Exception e) {
            log.error("重定向异常：{}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 从完整短链接中提取短链码
     *
     * @param fullShortUrl 完整短链接
     * @return 短链码
     */
    private String extractShortCode(String fullShortUrl) {
        if (fullShortUrl == null || fullShortUrl.isEmpty()) {
            return null;
        }
        int lastIndex = fullShortUrl.lastIndexOf("/");
        if (lastIndex != -1 && lastIndex < fullShortUrl.length() - 1) {
            return fullShortUrl.substring(lastIndex + 1);
        }
        return fullShortUrl;
    }

    /**
     * 三：查询短链接详细信息
     * GET /shortlink/info/{shortCode}
     *
     * @param shortCode 短链码
     * @return 短链接详细信息
     */
    @GetMapping("/info/{shortCode}")
    public R<ShortLinkVO> getInfo(@PathVariable String shortCode) {
        try {
            ShortLinkVO vo = shortLinkService.getInfo(shortCode);

            if (vo == null) {
                return R.fail("短链接不存在");
            }

            return R.ok("查询成功", vo);
        } catch (Exception e) {
            log.error("查询短链接异常：{}", shortCode, e);
            return R.fail("查询短链接失败");
        }
    }


    /**
     * 四：禁用短链接
     * POST /shortlink/disable
     * 需要 API Key 验证（简单的 Header 校验）
     *
     * @param shortCode 短链码（从请求参数获取）
     * @param apiKey API 密钥（从 Header 获取）
     * @return 操作结果
     */
    @PostMapping("/disable")
    public R<Void> disable(
            @RequestParam String shortCode,
            @RequestParam(required = false, defaultValue = "") String apiKey) {

        try {
            // 1. 简单的 API Key 验证
            if (!validateApiKey(apiKey)) {
                log.warn("禁用短链接失败 - API Key 无效：{}", apiKey);
                return R.fail(401, "API Key 无效或缺失");
            }

            // 2. 参数校验
            if (shortCode == null || shortCode.isBlank()) {
                return R.fail("短链码不能为空");
            }

            // 3. 执行禁用操作
            boolean success = shortLinkService.disable(shortCode);

            if (success) {
                return R.ok();
            } else {
                return R.fail("禁用失败，短链接可能不存在");
            }

        } catch (Exception e) {
            log.error("禁用短链接异常：{}", shortCode, e);
            return R.fail("禁用操作失败");
        }
    }

    /**
     * 简单的 API Key 验证方法
     * 实际项目中应该从配置文件或数据库中读取
     *
     * @param apiKey 请求中的 API Key
     * @return true=验证通过，false=验证失败
     */
    private boolean validateApiKey(String apiKey) {
        // 简单验证：非空且等于预设值
        // 实际应该从配置文件读取，比如：@Value("${api.key:}")
        String validApiKey = "your-secret-api-key"; // 预设的有效 API Key

        return apiKey != null && !apiKey.isBlank() && apiKey.equals(validApiKey);
    }
}
