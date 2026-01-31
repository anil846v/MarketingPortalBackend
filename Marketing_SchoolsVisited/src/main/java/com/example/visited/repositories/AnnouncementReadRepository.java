package com.example.visited.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.visited.entitys.Announcement;
import com.example.visited.entitys.AnnouncementRead;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Integer> {
	boolean existsByAnnouncementIdAndUserUserId(Integer announcementId, Integer userId);

	List<AnnouncementRead> findByAnnouncementId(Integer announcementId);

	long countByAnnouncementId(Integer announcementId);

	long countByAnnouncement(Announcement announcement);

	@Query("""
			   SELECT ar.announcement.id, COUNT(ar)
			   FROM AnnouncementRead ar
			   GROUP BY ar.announcement.id
			""")
	List<Object[]> countReadsGrouped();

	@Modifying
	@Query(value = "INSERT IGNORE INTO announcement_reads (announcement_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
	void insertIfNotExists(Integer announcementId, Integer userId);

	@Query("""
			   SELECT ar.announcement.id, COUNT(ar)
			   FROM AnnouncementRead ar
			   WHERE ar.announcement IN :announcements
			   GROUP BY ar.announcement.id
			""")
	List<Object[]> countReadsForAnnouncements(List<Announcement> announcements);

	@Query("SELECT ar FROM AnnouncementRead ar JOIN FETCH ar.user WHERE ar.announcement.id = ?1")
	List<AnnouncementRead> findByAnnouncementIdWithUser(Integer announcementId);

}
