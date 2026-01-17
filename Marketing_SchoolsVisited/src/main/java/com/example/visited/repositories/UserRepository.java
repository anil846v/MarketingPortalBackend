package com.example.visited.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.User;
import com.example.visited.entitys.User.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	User findByUsername(String username);
	User findByUserId(Integer userId);
	List<User> findByRole(Role marketing);
	
}