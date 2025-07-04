package com.hinadt.miaocha.config.security;

import com.hinadt.miaocha.domain.dto.user.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT认证过滤器 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT异常在request attribute中的key */
    public static final String JWT_EXCEPTION_ATTRIBUTE = "jwtException";

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = parseJwt(request);
        if (jwt != null) {
            try {
                jwtUtils.validateToken(jwt);

                // 从JWT token构造UserDTO对象
                UserDTO user = new UserDTO();
                user.setUid(jwtUtils.getUidFromToken(jwt));
                user.setId(jwtUtils.getUserIdFromToken(jwt));
                user.setNickname(jwtUtils.getNicknameFromToken(jwt));
                user.setEmail(jwtUtils.getEmailFromToken(jwt));
                user.setRole(jwtUtils.getRoleFromToken(jwt));
                user.setStatus(jwtUtils.getStatusFromToken(jwt));

                if (user.getStatus() == 1) {
                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug(
                            "Authentication set from JWT token, uid: {}, role: {}",
                            user.getUid(),
                            user.getRole());
                } else {
                    // 用户被禁用，记录异常信息让授权阶段处理
                    log.debug("User is disabled, uid: {}", user.getUid());
                    request.setAttribute(
                            JWT_EXCEPTION_ATTRIBUTE, new RuntimeException("User is disabled"));
                }

            } catch (Exception e) {
                request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        // 首先尝试从Authorization头获取token
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        // 如果Authorization头中没有token，尝试从查询参数获取（支持EventSource API）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }
}
