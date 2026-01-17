package com.example.visited.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        
        // Only rate limit communication endpoints
        if (path.contains("/messages/send") || path.contains("/announcements")) {
            String key = getClientKey(httpRequest);
            Bucket bucket = resolveBucket(key);
            
            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                // Add CORS headers for rate limit response
                httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.get(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        // 10 requests per minute
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientKey(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return userId != null ? userId : request.getRemoteAddr();
    }
}
