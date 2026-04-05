package com.example.mini_bank.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.mini_bank.util.ConstantsClass;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // allow swagger and openapi
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Too many requests. Try again later.");
        }
    }

    private Bucket newBucket(String key) {
        Refill refill = Refill.greedy(ConstantsClass.REQUESTS_PER_MINUTE_LIMIT, Duration.ofMinutes(1)); // 5 requests/min
        Bandwidth limit = Bandwidth.classic(5, refill);
        
        return Bucket4j.builder().addLimit(limit).build();
    }
}