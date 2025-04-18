package com.hina.log.config;

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
                .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
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

                        // 权限管理接口
                        .requestMatchers("/api/permissions/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // 数据源管理接口
                        .requestMatchers(HttpMethod.POST, "/api/datasources/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/datasources/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/datasources/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/datasources/**").authenticated()

                        // SQL查询和日志查询接口
                        .requestMatchers("/api/sql/**", "/api/logs/**").authenticated()

                        // 其他接口
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userService);
    }
}