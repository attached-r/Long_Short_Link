package rj.highlink.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/*
 * Swagger 配置类
 * 用于生成和自定义 API 接口文档
 */
@Configuration
public class SwaggerConfig {
    /**
     * 配置 OpenAPI 文档基本信息
     *
     * @return OpenAPI 对象，包含文档标题、描述、版本、联系人等信息
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("短链系统 API 文档")
                        .description("基于 Spring Boot + Redis 的短链服务")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("rj")
                                .email("2165329383@qq.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")));
    }
}