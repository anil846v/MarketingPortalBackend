package com.example.visited.entitys;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "modules")
public class Modules {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "module_name", nullable = false, length = 50)
	private String moduleName;
	
	@Column(columnDefinition = "TEXT")
	private String description;
	
	@Column(name = "is_active")
	private Boolean isActive = true;
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
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
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	
	public String getModuleName() { return moduleName; }
	public void setModuleName(String moduleName) { this.moduleName = moduleName; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public Boolean getIsActive() { return isActive; }
	public void setIsActive(Boolean isActive) { this.isActive = isActive; }
	
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}