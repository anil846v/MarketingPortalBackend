package com.example.visited.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.visited.entitys.Announcement;
import com.example.visited.entitys.Message;
import com.example.visited.entitys.AnnouncementRead;
import com.example.visited.repositories.AnnouncementRepository;
import com.example.visited.repositories.MessageRepository;
import com.example.visited.repositories.AnnouncementReadRepository;
import com.example.visited.repositories.UserRepository;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

@Service
@Transactional(timeout = 5)
public class CommunicationService {

    private static final Logger logger = LoggerFactory.getLogger(CommunicationService.class);
    private static final PolicyFactory XSS_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS).and(Sanitizers.BLOCKS);
    private final AnnouncementRepository announcementRepository;
    private final MessageRepository messageRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final UserRepository userRepository;

    public CommunicationService(AnnouncementRepository announcementRepository, MessageRepository messageRepository,
                                AnnouncementReadRepository announcementReadRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.messageRepository = messageRepository;
        this.announcementReadRepository = announcementReadRepository;
        this.userRepository = userRepository;
    }

    // ANNOUNCEMENTS
    public Map<String, Object> createAnnouncement(String title, String message, Integer adminId) {
        // Validation
        if (title == null || title.trim().length() < 3 || title.length() > 200) {
            throw new IllegalArgumentException("Title must be 3-200 characters");
        }
        if (message == null || message.trim().length() < 10 || message.length() > 5000) {
            throw new IllegalArgumentException("Message must be 10-5000 characters");
        }
        
        // XSS Protection
        String sanitizedTitle = XSS_POLICY.sanitize(title.trim());
        String sanitizedMessage = XSS_POLICY.sanitize(message.trim());
        
        Announcement announcement = new Announcement();
        announcement.setTitle(sanitizedTitle);
        announcement.setMessage(sanitizedMessage);
        announcement.setCreatedBy(adminId);
        
        Announcement saved = announcementRepository.save(announcement);
        logger.info("Announcement created: {}", saved.getId());
        
        return Map.of("message", "Announcement created", "id", saved.getId());
    }

    public List<Map<String, Object>> getAllAnnouncements() {
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Announcement a : announcements) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("title", a.getTitle());
            map.put("message", a.getMessage());
            map.put("createdAt", a.getCreatedAt());
            
            // Include read count
            long readCount = announcementReadRepository.countByAnnouncementId(a.getId());
            map.put("readCount", readCount);
            
            result.add(map);
        }
        
        return result;
    }

    public org.springframework.data.domain.Page<Map<String, Object>> getAllAnnouncementsPaginated(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Announcement> page = announcementRepository.findAll(org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), org.springframework.data.domain.Sort.by("createdAt").descending()));
        
        return page.map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("title", a.getTitle());
            map.put("message", a.getMessage());
            map.put("createdAt", a.getCreatedAt());
            
            // Include read count for admin
            long readCount = announcementReadRepository.countByAnnouncementId(a.getId());
            map.put("readCount", readCount);
            
            return map;
        });
    }

    // MESSAGES
    public Map<String, Object> sendMessage(Integer senderId, Integer receiverId, String message) {
        // Validation
        if (message == null || message.trim().isEmpty() || message.length() > 2000) {
            throw new IllegalArgumentException("Message must be 1-2000 characters");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }
        
        // XSS Protection
        String sanitizedMessage = XSS_POLICY.sanitize(message.trim());
        
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setMessage(sanitizedMessage);
        
        Message saved = messageRepository.save(msg);
        logger.info("Message sent from {} to {}", senderId, receiverId);
        
        return Map.of("message", "Message sent", "id", saved.getId());
    }

    public List<Map<String, Object>> getConversation(Integer userId1, Integer userId2) {
        List<Message> messages = messageRepository.findConversation(userId1, userId2);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Message m : messages) {
            // Skip if deleted for everyone
            if (m.getDeletedForEveryone()) {
                continue;
            }
            
            // Skip if deleted by current user (delete for me)
            if ((m.getSenderId().equals(userId1) && m.getDeletedBySender()) ||
                (m.getReceiverId().equals(userId1) && m.getDeletedByReceiver())) {
                continue;
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderId", m.getSenderId());
            map.put("receiverId", m.getReceiverId());
            map.put("message", m.getMessage());
            map.put("isRead", m.getIsRead());
            map.put("createdAt", m.getCreatedAt());
            result.add(map);
        }
        
        return result;
    }

    public void markAsRead(Integer messageId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            messageRepository.save(msg);
        });
    }

    public void markConversationAsRead(Integer currentUserId, Integer otherUserId) {
        messageRepository.markConversationAsReadBatch(currentUserId, otherUserId);
    }

    public Long getUnreadCount(Integer userId) {
        return messageRepository.countByReceiverIdAndIsReadFalseAndDeletedByReceiverFalseAndDeletedForEveryoneFalse(userId);
    }

    public void deleteMessage(Integer messageId, Integer userId, String deleteType) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            // User can delete messages they sent OR received
            if (!msg.getSenderId().equals(userId) && !msg.getReceiverId().equals(userId)) {
                throw new IllegalArgumentException("You can only delete messages you sent or received");
            }
            
            if ("FOR_EVERYONE".equals(deleteType)) {
                // Only sender can delete for everyone
                if (!msg.getSenderId().equals(userId)) {
                    throw new IllegalArgumentException("You can only delete for everyone if you sent the message");
                }
                msg.setDeletedForEveryone(true);
                messageRepository.save(msg);
                logger.info("Message {} deleted for everyone by user {}", messageId, userId);
            } else {
                // Delete for me only - both sender and receiver can do this
                if (msg.getSenderId().equals(userId)) {
                    msg.setDeletedBySender(true);
                } else if (msg.getReceiverId().equals(userId)) {
                    msg.setDeletedByReceiver(true);
                }
                messageRepository.save(msg);
                logger.info("Message {} deleted for user {}", messageId, userId);
            }
        });
    }

    public void deleteAnnouncement(Integer announcementId, Integer adminId) {
        announcementRepository.findById(announcementId).ifPresent(announcement -> {
            // Only admin who created can delete
            if (announcement.getCreatedBy().equals(adminId)) {
                announcementRepository.delete(announcement);
                logger.info("Announcement {} deleted by admin {}", announcementId, adminId);
            } else {
                throw new IllegalArgumentException("You can only delete your own announcements");
            }
        });
    }

    public void markAnnouncementAsRead(Integer announcementId, Integer userId) {
        announcementReadRepository.insertIfNotExists(announcementId, userId);
    }

    public Map<String, Object> getAnnouncementReadStatus(Integer announcementId) {
        List<AnnouncementRead> reads = announcementReadRepository.findByAnnouncementId(announcementId);
        
        // Get all user IDs at once
        List<Integer> userIds = reads.stream().map(AnnouncementRead::getUserId).toList();
        List<com.example.visited.entitys.User> users = userRepository.findAllById(userIds);
        
        // Create map for quick lookup
        Map<Integer, com.example.visited.entitys.User> userMap = new HashMap<>();
        for (com.example.visited.entitys.User user : users) {
            userMap.put(user.getUserId(), user);
        }
        
        List<Map<String, Object>> readBy = new ArrayList<>();
        for (AnnouncementRead read : reads) {
            com.example.visited.entitys.User user = userMap.get(read.getUserId());
            if (user != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getUserId());
                map.put("username", user.getUsername());
                map.put("readAt", read.getReadAt());
                readBy.add(map);
            }
        }
        
        return Map.of(
            "announcementId", announcementId,
            "totalReads", reads.size(),
            "readBy", readBy
        );
    }
}
