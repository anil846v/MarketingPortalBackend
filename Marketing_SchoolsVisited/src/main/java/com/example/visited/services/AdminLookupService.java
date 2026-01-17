package com.example.visited.services;

import com.example.visited.entitys.User;
import com.example.visited.repositories.UserRepository;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class AdminLookupService {
    
    private final UserRepository userRepository;
    private volatile Integer cachedAdminId;
    
    public AdminLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @PostConstruct
    public void init() {
        cachedAdminId = userRepository.findByRole(User.Role.ADMIN).stream()
                .findFirst()
                .map(User::getUserId)
                .orElse(null);
    }
    
    public Integer getAdminUserId() {
        if (cachedAdminId == null) {
            cachedAdminId = userRepository.findByRole(User.Role.ADMIN).stream()
                    .findFirst()
                    .map(User::getUserId)
                    .orElse(null);
        }
        if (cachedAdminId == null) {
            throw new IllegalStateException("No admin user found in system");
        }
        return cachedAdminId;
    }
}
