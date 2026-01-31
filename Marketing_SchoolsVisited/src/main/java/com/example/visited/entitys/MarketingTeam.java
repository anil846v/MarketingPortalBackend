package com.example.visited.entitys;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_team")
public class MarketingTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Hibernate will handle the FK
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String designation;

    @Column(name = "assigned_region", length = 100)
    private String assignedRegion;

    @Column(name = "target_districts", columnDefinition = "TEXT")
    private String targetDistricts;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; } // important: this sets user_id

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getAssignedRegion() { return assignedRegion; }
    public void setAssignedRegion(String assignedRegion) { this.assignedRegion = assignedRegion; }

    public String getTargetDistricts() { return targetDistricts; }
    public void setTargetDistricts(String targetDistricts) { this.targetDistricts = targetDistricts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }

}
