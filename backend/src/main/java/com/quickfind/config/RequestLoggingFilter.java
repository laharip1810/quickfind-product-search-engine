package com.quickfind.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs one line per API request (method, path, query, status, duration) and tags every
 * log line of that request with a request id (MDC), which is also returned in the
 * X-Request-Id header. Request bodies and headers are never logged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        long start = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
            log.info("{} {}{} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), query,
                    response.getStatus(), tookMs);
            MDC.remove(MDC_KEY);
        }
    }

    /** Accept a caller-supplied id only if it is short and harmless; otherwise generate one. */
    private static String sanitize(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9-]{1,64}")) {
            return candidate;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
