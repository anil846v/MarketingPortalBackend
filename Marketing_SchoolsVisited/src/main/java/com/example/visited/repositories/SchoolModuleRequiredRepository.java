package com.example.visited.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.visited.entitys.SchoolModuleRequired;
import com.example.visited.entitys.SchoolVisited;

import jakarta.transaction.Transactional;

@Repository
public interface SchoolModuleRequiredRepository extends JpaRepository<SchoolModuleRequired, Integer> {
    List<SchoolModuleRequired> findBySchoolVisitedId(Integer schoolVisitedId);
    List<SchoolModuleRequired> findByModuleId(Integer moduleId);
    List<SchoolModuleRequired> findBySchoolVisited(SchoolVisited schoolVisited);

    
    @Modifying
    @Transactional
    @Query("DELETE FROM SchoolModuleRequired s WHERE s.schoolVisited = :visit")
    void deleteBySchoolVisited(@Param("visit") SchoolVisited visit);
    Optional<SchoolModuleRequired> findBySchoolVisitedIdAndModuleId(Integer schoolVisitedId, Integer moduleId);

    

}