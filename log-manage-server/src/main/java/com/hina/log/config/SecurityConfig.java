package com.hina.log.config;

import com.hina.log.security.CustomAccessDeniedHandler;
import com.hina.log.security.CustomAuthenticationEntryPoint;
import com.hina.log.security.JwtAuthenticationFilter;
import com.hina.log.security.JwtUtils;
import com.hina.log.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 安全配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public SecurityConfig(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler()))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // 前端静态资源和SPA路由
                        .requestMatchers("/", "/index.html", "/static/favicon.ico", "/manifest.json").permitAll()
                        .requestMatchers("/assets/**", "/static/**", "/css/**", "/js/**", "/img/**", "/fonts/**")
                        .permitAll()

                        // 公开接口
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**",
                                "/api/doc/**", "/api-docs", "/swagger-resources/**", "/webjars/**")
                        .permitAll()

                        // 用户自己的信息和修改自己密码的接口
                        .requestMatchers("/api/users/me", "/api/users/password").authenticated()

                        // 其他用户管理接口
                        .requestMatchers("/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // 用户查看自己权限的接口
                        .requestMatchers("/api/permissions/my/tables").authenticated()

                        // 其他权限管理接口
                        .requestMatchers("/api/permissions/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // 数据源管理接口
                        .requestMatchers(HttpMethod.POST, "/api/datasources/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/datasources/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/datasources/**").authenticated()

                        // Logstash进程管理接口，只有管理员可以操作
                        .requestMatchers("/api/logstash/processes/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // 机器管理接口
                        .requestMatchers(HttpMethod.GET, "/api/machines").authenticated()
                        .requestMatchers("/api/machines/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // SQL查询和日志查询接口需要认证
                        .requestMatchers("/api/logs/**", "/api/sql/**").authenticated()

                        // 所有其他API接口需要认证
                        .requestMatchers("/api/**").authenticated()

                        // 所有前端路由直接放行，由前端处理
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userService);
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }
}