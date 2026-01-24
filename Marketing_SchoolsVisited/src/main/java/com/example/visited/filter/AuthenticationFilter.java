package com.example.visited.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.visited.entitys.User;
import com.example.visited.entitys.User.Role;
import com.example.visited.repositories.UserRepository;
import com.example.visited.services.AuthService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
@WebFilter(urlPatterns = {"/api/*", "/admin/*","/auth/*"})
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    

    @Value("${cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    private static final String[] PUBLIC_PATHS = {
        "/auth/login"
    		
        // add more public endpoints if needed: register, forgot-password, etc.
    };

    public AuthenticationFilter(AuthService authService, UserRepository userRepository) {
    	logger.info("Authentication filter initialized");
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("Processing {} {}", method, requestURI);

        // 1. Handle CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            setCorsHeaders(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 2. Allow public endpoints
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Get + validate token
        String token = extractToken(request);
        if (token == null || !authService.validateToken(token)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing authentication token");
            return;
        }

        // 4. Get username + user
        String username = authService.extractUsername(token);
        User user = userRepository.findByUsername(username);

        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not found");
            return;
        }

        // 5. Critical: Check account status
        if (user.getStatus() != User.Status.Approved) {   // ← assuming you have Status enum
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Account is not approved");
            return;
        }

        // 6. Role-based authorization
        boolean isAdminPath = requestURI.startsWith("/admin/");
        boolean isApiPath = requestURI.startsWith("/api/");

        if (isAdminPath && user.getRole() != Role.ADMIN) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        // Most likely you want BOTH admin + marketing to access /api/*
        if (isApiPath && user.getRole() != Role.ADMIN && user.getRole() != Role.MARKETING) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Marketing or Admin access required");
            return;
        }

        // 7. Success → attach user to request
        request.setAttribute("authenticatedUser", user);
        logger.debug("Authenticated user: {} ({})", username, user.getRole());

        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        return Arrays.stream(PUBLIC_PATHS).anyMatch(uri::equals) || uri.startsWith("/health");
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> "authToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
    private static final ObjectMapper JSON = new ObjectMapper();
//    / Then in sendError:
    	private void sendError(HttpServletResponse res, int status, String msg) throws IOException {
    	    res.setStatus(status);
    	    res.setContentType("application/json");
    	    res.setCharacterEncoding("UTF-8");
    	    
    	    Map<String, Object> error = Map.of(
    	        "status", status,
    	        "error", status == 401 ? "Unauthorized" : "Forbidden",
    	        "message", msg,
    	        "timestamp", Instant.now().toString()
    	    );
    	    
    	    JSON.writeValue(res.getWriter(), error);
    	}
}