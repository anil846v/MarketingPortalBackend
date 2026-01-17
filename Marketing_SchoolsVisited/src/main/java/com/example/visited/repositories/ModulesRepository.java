package com.example.visited.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.Modules;
import java.util.List;

@Repository
public interface ModulesRepository extends JpaRepository<Modules, Integer> {
	List<Modules> findByIsActiveTrue();
}
