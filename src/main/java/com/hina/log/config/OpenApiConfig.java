package com.hina.log.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置类
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("海纳日志系统API")
                        .version("1.0.0")
                        .description("海纳日志系统API文档，提供了日志管理、数据源配置、日志查询等功能的详细接口说明")
                        .contact(new Contact()
                                .name("海纳科技")
                                .email("support@hina.com")
                                .url("https://www.hina.com"))
                        .license(new License()
                                .name("Private License")
                                .url("https://www.hina.com/licenses")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.logs.hina.com")
                                .description("生产环境")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("使用JWT Token进行认证，在请求头中添加 Authorization: Bearer {token}")));
    }

}
