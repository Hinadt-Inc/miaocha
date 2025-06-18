package com.hinadt.miaocha.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC配置 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentUserMethodArgumentResolver);
    }

    /** 配置静态资源处理器 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置前端静态资源 - assets目录
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:assets/")
                .setCachePeriod(86400); // 24小时缓存

        // 配置Monaco Editor资源
        registry.addResourceHandler("/monaco-editor/**")
                .addResourceLocations("classpath:monaco-editor/")
                .setCachePeriod(86400);

        // 配置根目录资源（favicon.ico, logo.png等）
        registry.addResourceHandler("/*.ico", "/*.png", "/*.jpg", "/*.svg", "/*.json")
                .addResourceLocations("classpath:")
                .setCachePeriod(86400);

        // 配置Swagger资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .setCachePeriod(3600);
    }

    /** 添加视图控制器，处理SPA的前端路由 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径映射到index.html
        registry.addViewController("/").setViewName("forward:/index.html");

        // 注意：不在这里配置SPA路由回退
        // 因为Spring Boot会按照以下优先级处理请求：
        // 1. Controller (@RequestMapping)
        // 2. 静态资源处理器 (addResourceHandlers)
        // 3. 视图控制器 (addViewControllers)
        // 4. 默认处理 (FallbackController)
        //
        // 对于SPA路由如 /system/logstash：
    }

    /** 配置CORS跨域 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
