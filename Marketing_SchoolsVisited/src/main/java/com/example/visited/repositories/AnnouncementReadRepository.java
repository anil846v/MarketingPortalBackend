package com.example.visited.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.AnnouncementRead;
import java.util.List;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Integer> {
    boolean existsByAnnouncementIdAndUserId(Integer announcementId, Integer userId);
    List<AnnouncementRead> findByAnnouncementId(Integer announcementId);
    long countByAnnouncementId(Integer announcementId);
    
    @Modifying
    @Query(value = "INSERT IGNORE INTO announcement_reads (announcement_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    void insertIfNotExists(Integer announcementId, Integer userId);
}
