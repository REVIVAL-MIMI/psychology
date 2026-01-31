package com.psychology.security;

import com.psychology.model.entity.User;
import com.psychology.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String requestUri = request.getRequestURI();

            log.debug("Processing request: {} {}", request.getMethod(), requestUri);

            // Пропускаем публичные эндпоинты
            if (requestUri.startsWith("/api/v1/auth/") ||
                    requestUri.startsWith("/api/v1/invites/validate/") ||
                    requestUri.startsWith("/api/v1/test/") ||
                    requestUri.startsWith("/api/v1/debug/") ||
                    requestUri.equals("/error")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No Bearer token found for protected endpoint: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(jwt)) {
                log.warn("Invalid or expired token");
                filterChain.doFilter(request, response);
                return;
            }

            final String phone = jwtTokenProvider.extractUsername(jwt);
            log.debug("Extracted phone from token: {}", phone);

            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Загружаем пользователя из базы
                User user = userRepository.findByPhone(phone)
                        .orElse(null);

                if (user != null) {
                    log.debug("Loaded user: ID={}, Role={}", user.getId(), user.getRole());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set successfully for user: {}", phone);
                }
            }
        } catch (Exception e) {
            log.error("Error in JWT filter: ", e);
        }

        filterChain.doFilter(request, response);
    }
}