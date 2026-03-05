package rj.highlink.entity.dto;

import lombok.Data;

//spring-boot-starter-validation 依赖的作用是提供参数验证功能

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 创建短链接的参数类
 */
@Data
public class CreateShortLinkDTO {

    @NotBlank(message = "原始链接不能为空")
    @Pattern(regexp = "^(https?|ftp)://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$", // 正则，匹配 http(s):// 开头的 URL
            message = "原始链接格式不正确")
    private String longUrl;

    private LocalDateTime expireTime;  // 失效时间
}

/*
 * 创建短链接的参数类

 * 这里用来接收前端用户提交的参数，并做参数校验

 * * DTO 是 “数据传输的载体”，解决跨层传输的冗余问题，按需定义字段；
 */