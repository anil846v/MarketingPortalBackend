package com.example.visited.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.SchoolModuleRequired;
import java.util.List;

@Repository
public interface SchoolModuleRequiredRepository extends JpaRepository<SchoolModuleRequired, Integer> {
    List<SchoolModuleRequired> findBySchoolVisitedId(Integer schoolVisitedId);
    List<SchoolModuleRequired> findByModuleId(Integer moduleId);
}