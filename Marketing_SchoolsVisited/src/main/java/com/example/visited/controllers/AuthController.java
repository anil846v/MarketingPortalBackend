package com.example.visited.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.visited.DTO.LoginRequest;
import com.example.visited.entitys.User;
import com.example.visited.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "${cors.allowed-origin:http://localhost:5173}", allowCredentials = "true")
@RequestMapping("/auth")
public class AuthController {
	 
	private final AuthService authService;
	
	 public AuthController(AuthService authService) {     
		                                                  //HttpServletRequest request  - READ incoming data  for CRUD
                                                          //HttpServletResponse response - WRITE outgoing data  for login/logout

		this.authService = authService;
	}

	
	 @PostMapping("/login")
	    @CrossOrigin
	    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
	        try {
	            User user = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
	            
	            // Block pending/rejected users
	            if (user.getStatus() != User.Status.Approved) {
	                Map<String, Object> responseBody = new HashMap<>();
	                responseBody.put("error", "User not approved");
	                responseBody.put("message", "Please wait for admin approval");
	                responseBody.put("status", user.getStatus().name().toLowerCase());
	                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseBody);
	            }
	            
	            String token = authService.generateToken(user);

	            Cookie cookie = new Cookie("authToken", token);
	            cookie.setHttpOnly(true);
	            cookie.setSecure(false); // Set to true if using HTTPS
	            cookie.setPath("/");
	            cookie.setMaxAge(3600); // 1 hour
	            cookie.setDomain("localhost");
	            response.addCookie(cookie);
	           // Optional but useful
	            
	            response.addHeader("Set-Cookie",
	                    String.format("authToken=%s; HttpOnly; Path=/; Max-Age=3600; SameSite=None", token));

	            
	            Map<String, Object> responseBody = new HashMap<>();
	            responseBody.put("message", "Login successful");
	            responseBody.put("role", user.getRole().name());
	            responseBody.put("username", user.getUsername());

	            return ResponseEntity.ok(responseBody);
	            
	        } 
	        catch (RuntimeException e) 
	        {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	        }
	    }
@PostMapping("/logout")
public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
	try {
		// Get authenticated user
		User user = (User) request.getAttribute("authenticatedUser");
		
		// Delete token from database if user exists
		if (user != null) {
			authService.logout(user);
		}
		
		// Clear cookie (always do this, even if user is null)
		Cookie authCookie = new Cookie("authToken", "");
		authCookie.setHttpOnly(true);
		authCookie.setSecure(false);
		authCookie.setPath("/");
		authCookie.setMaxAge(0);
		authCookie.setDomain("localhost");
		response.addCookie(authCookie);
		
		// Also add Set-Cookie header for better compatibility
		response.addHeader("Set-Cookie", 
			"authToken=; HttpOnly; Path=/; Max-Age=0; Domain=localhost");

		return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
	} catch (Exception e) {
		// Even if error, try to clear cookie
		Cookie authCookie = new Cookie("authToken", "");
		authCookie.setHttpOnly(true);
		authCookie.setPath("/");
		authCookie.setMaxAge(0);
		response.addCookie(authCookie);
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Logout failed: " + e.getMessage()));
	}
}

@PostMapping("/validate")
public ResponseEntity<?> validateSession(HttpServletRequest request) {
	User user = (User) request.getAttribute("authenticatedUser");
	if (user == null) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("valid", false, "error", "Session expired"));
	}
	return ResponseEntity.ok(Map.of(
		"valid", true,
		"role", user.getRole().name(),
		"username", user.getUsername()
	));
}

}
