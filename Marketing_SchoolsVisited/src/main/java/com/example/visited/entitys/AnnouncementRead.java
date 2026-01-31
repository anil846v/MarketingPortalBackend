package com.example.visited.entitys;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "announcement_reads",
       uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "user_id"}))
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        readAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
