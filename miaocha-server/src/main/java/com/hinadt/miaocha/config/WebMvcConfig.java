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
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserMethodArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /** 添加静态资源处理器 注意：顺序很重要，要确保Swagger相关资源先被处理 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 特别处理/swagger-ui路径 - 必须放在最前面
        // 其他Swagger相关资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/swagger-ui/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/v3/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600);

        // 应用的静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }

    /** 添加视图控制器，处理SPA的前端路由 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径映射到index.html
        registry.addViewController("/").setViewName("forward:/index.html");

        // 注意：这里使用正则排除api和带点的路径，但不使用否定前瞻(?!)，避免捕获组问题
        // 匹配所有不带点(.)的路径，并且不以api、swagger-ui等开头的路径
        registry.addViewController("/{path:[^.]*}").setViewName("forward:/index.html");
    }
}
