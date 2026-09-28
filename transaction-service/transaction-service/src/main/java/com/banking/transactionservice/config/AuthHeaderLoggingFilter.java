package com.banking.transactionservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TEMPORARY debug filter: confirms the gateway's TokenRelay is forwarding the bearer token.
 * Runs before Spring Security (highest precedence) so it logs even when the request
 * is later rejected with 401. Delete this class once you've verified the header arrives.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthHeaderLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null) {
            log.info("[AUTH-DEBUG] {} {} -> NO Authorization header", request.getMethod(), request.getRequestURI());
        } else if (header.startsWith("Bearer ")) {
            String token = header.substring(7);
            // A JWT has 3 dot-separated parts; an opaque token does not.
            boolean looksLikeJwt = token.split("\\.").length == 3;
            log.info("[AUTH-DEBUG] {} {} -> Bearer token present, looksLikeJwt={}, length={}, starts with '{}...'",
                    request.getMethod(), request.getRequestURI(), looksLikeJwt, token.length(),
                    token.substring(0, Math.min(10, token.length())));
            log.info("token: "+ token);
        } else {
            log.info("[AUTH-DEBUG] {} {} -> Authorization header present but not Bearer", request.getMethod(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
