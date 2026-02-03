package com.psychology.security;

import com.psychology.model.entity.User;
import com.psychology.model.entity.Psychologist;
import com.psychology.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Collections;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${admin.login:admin}")
    private String adminLogin;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicEndpoint(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String requestUri = request.getRequestURI();

            log.debug("Processing request: {} {}", request.getMethod(), requestUri);

            // Пропускаем публичные эндпоинты
            if (isPublicEndpoint(requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No Bearer token found for protected endpoint: {}", requestUri);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authorization header missing or invalid");
                return;
            }

            final String jwt = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(jwt)) {
                log.warn("Invalid or expired token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token is invalid or expired");
                return;
            }

            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jwt))) {
                log.warn("Blacklisted token used for protected endpoint: {}", requestUri);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token is blacklisted");
                return;
            }

            final String phone = jwtTokenProvider.extractUsername(jwt);
            log.debug("Extracted phone from token: {}", phone);

            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Загружаем пользователя из базы
                User user = userRepository.findByPhone(phone)
                        .orElse(null);

                if (user != null) {
                    if (user instanceof Psychologist psychologist && !psychologist.isVerified()) {
                        if (!isVerificationAllowedEndpoint(requestUri)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("Psychologist account is pending verification");
                            return;
                        }
                    }
                    log.debug("Loaded user: ID={}, Role={}", user.getId(), user.getRole());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set successfully for user: {}", phone);
                } else if (phone.equals(adminLogin)) {
                    var adminUser = org.springframework.security.core.userdetails.User
                            .withUsername(adminLogin)
                            .password("")
                            .roles("ADMIN")
                            .build();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            adminUser,
                            null,
                            adminUser.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set successfully for admin: {}", adminLogin);
                }
            }
        } catch (Exception e) {
            log.error("Error in JWT filter: ", e);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String requestUri) {
        return requestUri.startsWith("/api/v1/auth/") ||
                requestUri.startsWith("/api/v1/admin/login") ||
                requestUri.startsWith("/api/v1/admin/refresh") ||
                requestUri.equals("/admin.html") ||
                requestUri.equals("/admin-login.html") ||
                requestUri.equals("/favicon.ico") ||
                requestUri.startsWith("/static/") ||
                requestUri.startsWith("/api/v1/invites/validate/") ||
                requestUri.startsWith("/api/v1/test/") ||
                requestUri.startsWith("/api/v1/debug/") ||
                requestUri.startsWith("/ws-chat") ||
                requestUri.equals("/error");
    }

    private boolean isVerificationAllowedEndpoint(String requestUri) {
        return requestUri.startsWith("/api/v1/profile/verification-status") ||
                requestUri.startsWith("/api/v1/auth/logout") ||
                requestUri.startsWith("/api/v1/auth/refresh");
    }
}
