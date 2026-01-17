package com.example.visited.entitys;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "jwt_tokens")
public class JWT {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	// ── Now using real relationship instead of plain Integer ──
    @ManyToOne(fetch = FetchType.LAZY)  // LAZY is usually better for performance
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

	@Column(nullable = false, length = 1000)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public JWT() {
	}

	public JWT( User user, String token, LocalDateTime expiresAt) {

		this.user = user;
		this.token = token;
		this.expiresAt = expiresAt;
	}

//	@PrePersist
//	protected void onCreate() {
//		createdAt = LocalDateTime.now();
//		updatedAt = LocalDateTime.now();
//	}                                         // AUTO UPDATE
//	
//	@PreUpdate
//	protected void onUpdate() {
//		updatedAt = LocalDateTime.now();
//	}
	


	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setUserId(Integer id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}