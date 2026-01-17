package com.example.visited.entitys;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "sender_id", nullable = false)
    private Integer senderId;
    
    @Column(name = "receiver_id", nullable = false)
    private Integer receiverId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;
    
    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;
    
    @Column(name = "deleted_for_everyone")
    private Boolean deletedForEveryone = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getDeletedBySender() {
        return deletedBySender;
    }

    public void setDeletedBySender(Boolean deletedBySender) {
        this.deletedBySender = deletedBySender;
    }

    public Boolean getDeletedByReceiver() {
        return deletedByReceiver;
    }

    public void setDeletedByReceiver(Boolean deletedByReceiver) {
        this.deletedByReceiver = deletedByReceiver;
    }

    public Boolean getDeletedForEveryone() {
        return deletedForEveryone;
    }

    public void setDeletedForEveryone(Boolean deletedForEveryone) {
        this.deletedForEveryone = deletedForEveryone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
