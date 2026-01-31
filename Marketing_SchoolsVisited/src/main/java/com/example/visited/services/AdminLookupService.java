package com.example.visited.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.visited.entitys.User;
import com.example.visited.repositories.UserRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminLookupService {
    
    private final UserRepository userRepository;
    private volatile Integer cachedAdminId;
    
    public AdminLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
//    @PostConstruct
//    public void init() {
//        cachedAdminId = userRepository.findByRoleOrderByUserIdAsc(User.Role.ADMIN)
//                .stream()
//                .findFirst()
//                .map(User::getUserId)
//                .orElse(null);
//    }

    @Transactional(readOnly = true)
    public Integer getAdminUserId() {
        if (cachedAdminId != null) return cachedAdminId;

        Optional<User> adminUser = userRepository.findByRoleOrderByUserIdAsc(User.Role.ADMIN)
                .stream()
                .findFirst();

        cachedAdminId = adminUser
                .map(User::getUserId)
                .orElseThrow(() -> new IllegalStateException("No admin user found in system"));

        return cachedAdminId;
    }
}
