package com.example.visited.entitys;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_modules_required")
public class SchoolModuleRequired {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "school_visited_id", nullable = false)
	private Integer schoolVisitedId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "school_visited_id", insertable = false, updatable = false)
	private SchoolVisited schoolVisited;
	
	@Column(name = "module_id", nullable = false)
	private Integer moduleId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "module_id", insertable = false, updatable = false)
	private Modules module;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "is_selected")
	private IsSelected isSelected = IsSelected.No;
	
	private String remarks;
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	public enum IsSelected {
		Yes, No
	}
	
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
	
	public Integer getSchoolVisitedId() { return schoolVisitedId; }
	public void setSchoolVisitedId(Integer schoolVisitedId) { this.schoolVisitedId = schoolVisitedId; }
	
	public Integer getModuleId() { return moduleId; }
	public void setModuleId(Integer moduleId) { this.moduleId = moduleId; }
	
	public IsSelected getIsSelected() { return isSelected; }
	public void setIsSelected(IsSelected isSelected) { this.isSelected = isSelected; }
	
	public String getRemarks() { return remarks; }
	public void setRemarks(String remarks) { this.remarks = remarks; }
	
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
