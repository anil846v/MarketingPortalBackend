package com.example.visited.services;

import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.visited.entitys.JWT;
import com.example.visited.entitys.User;
import com.example.visited.repositories.JWTTokenRepository;
import com.example.visited.repositories.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
	private final Key SIGNING_KEY;

	private final UserRepository userRepository;
	private final JWTTokenRepository jwtTokenRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, JWTTokenRepository jwtTokenRepository,
			@Value("${jwt.secret}") String jwtSecret) {
		this.userRepository = userRepository;
		this.jwtTokenRepository = jwtTokenRepository;
		this.passwordEncoder = new BCryptPasswordEncoder();

// Ensure the key length is at least 64 bytes
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
			throw new IllegalArgumentException(
					"JWT_SECRET in application.properties must be at least 64 bytes long for HS512.");
		}
		this.SIGNING_KEY = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

	public User authenticate(String username, String password) {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			logger.warn("Authentication failed: User not found - {}", username);
			throw new RuntimeException("Invalid username or password");
		}

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			logger.warn("Authentication failed: Invalid password for user - {}", username);
			throw new RuntimeException("Invalid username or password");
		}
		
		logger.info("User authenticated successfully: {}", username);
		return user;
	}

	    public String generateToken(User user) {
	        String token;
	        LocalDateTime now = LocalDateTime.now();
	        JWT existingToken = jwtTokenRepository.findByUser(user);

	        if (existingToken != null && now.isBefore(existingToken.getExpiresAt())) {
	            token = existingToken.getToken();
	        } else {
	            token = generateNewToken(user);
	            if (existingToken != null) {
	                jwtTokenRepository.delete(existingToken);
	            }
	            saveToken(user, token);
	        }
	        return token;
	    }

	    private String generateNewToken(User user) {
	        return Jwts.builder()
	                .setSubject(user.getUserId().toString())  // now immutable ID
	                .claim("role", user.getRole().name())
	                .setIssuedAt(new Date())
	                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
	                .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
	                .compact();
	    }

	    public void saveToken(User user, String token) {
	    	 LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
	 	    JWT jwt = new JWT(user, token, expiresAt);
	 	    jwtTokenRepository.save(jwt);
	 	
	    }

	@Transactional
	    public void logout(User user) {
	     // Option A: Delete ALL tokens of this user (single session policy)
	        jwtTokenRepository.deleteByUser(user);

	        // Option B: If you want to keep other sessions alive (more common in 2025+)
	        // Do nothing here - or delete only specific token if you pass it
	        // jwtTokenRepository.deleteByToken(currentToken);  ← if you pass token
	    }

	    public boolean validateToken(String token) {
	        try {
	            logger.debug("Validating JWT token");

	            // Parse and validate the token
	            Jwts.parserBuilder()
	                .setSigningKey(SIGNING_KEY)
	                .build()
	                .parseClaimsJws(token);

	            // Check if the token exists in the database and is not expired
	            Optional<JWT> jwtToken = jwtTokenRepository.findByToken(token);
	            if (jwtToken.isPresent()) {
	                boolean isValid = jwtToken.get().getExpiresAt().isAfter(LocalDateTime.now());
	                logger.debug("Token validation result: {}", isValid);
	                return isValid;
	            }

	            logger.debug("Token not found in database");
	            return false;
	        } catch (Exception e) {
	            logger.warn("Token validation failed: {}", e.getMessage());
	            return false;
	        }
	    }

	    public Integer extractUserId(String token) {
	        try {
	            String userIdStr = Jwts.parserBuilder()
	                    .setSigningKey(SIGNING_KEY)
	                    .build()
	                    .parseClaimsJws(token)
	                    .getBody()
	                    .getSubject();
	            return Integer.parseInt(userIdStr);
	        } catch (Exception e) {
	            throw new SecurityException("Invalid token");
	        }
	    }

}
