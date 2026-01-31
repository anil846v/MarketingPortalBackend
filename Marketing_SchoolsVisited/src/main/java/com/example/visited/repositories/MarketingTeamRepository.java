package com.example.visited.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.visited.entitys.MarketingTeam;
import com.example.visited.entitys.User;

@Repository
public interface MarketingTeamRepository extends JpaRepository<MarketingTeam, Long> {
    MarketingTeam findByUser_UserId(Integer userId);
	MarketingTeam findByEmail(String email);
	MarketingTeam findByPhoneNumber(String phoneNumber);
//	void deleteByStudent(MarketingTeam student);
	Optional<MarketingTeam> findByUser(User user);
}