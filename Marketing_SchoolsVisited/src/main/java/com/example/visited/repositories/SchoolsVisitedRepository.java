package com.example.visited.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.SchoolVisited;
import java.util.List;

@Repository
public interface SchoolsVisitedRepository extends JpaRepository<SchoolVisited, Integer> {
	List<SchoolVisited> findByUserId(Integer userId);
	List<SchoolVisited> findByMarketingExecutiveName(String marketingExecutiveName);
	List<SchoolVisited> findByStatus(SchoolVisited.VisitStatus status);
	List<SchoolVisited> findByLocationCity(String locationCity);
}