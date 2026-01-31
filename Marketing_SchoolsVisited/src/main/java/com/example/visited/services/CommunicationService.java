package com.example.visited.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.visited.entitys.Announcement;
import com.example.visited.entitys.AnnouncementRead;
import com.example.visited.entitys.Message;
import com.example.visited.entitys.User;
import com.example.visited.repositories.AnnouncementReadRepository;
import com.example.visited.repositories.AnnouncementRepository;
import com.example.visited.repositories.MessageRepository;
import com.example.visited.repositories.UserRepository;
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

        if (title == null || title.trim().length() < 3 || title.length() > 200) {
            throw new IllegalArgumentException("Title must be 3-200 characters");
        }
        if (message == null || message.trim().length() < 10 || message.length() > 5000) {
            throw new IllegalArgumentException("Message must be 10-5000 characters");
        }

        String sanitizedTitle = XSS_POLICY.sanitize(title.trim());
        String sanitizedMessage = XSS_POLICY.sanitize(message.trim());

        // 🔥 NEW — fetch User entity
        User admin = userRepository.findByUserId(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only ADMIN can create announcements");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(sanitizedTitle);
        announcement.setMessage(sanitizedMessage);

        // ✅ RELATIONSHIP, NOT ID
        announcement.setCreatedBy(admin);

        Announcement saved = announcementRepository.save(announcement);
        logger.info("Announcement created by admin {}: {}", adminId, saved.getId());

        return Map.of(
                "message", "Announcement created",
                "id", saved.getId()
        );
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
            long readCount = announcementReadRepository.countByAnnouncement(a);
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
            long readCount = announcementReadRepository.countByAnnouncement(a);
            map.put("readCount", readCount);
            
            return map;
        });
    }

    // MESSAGES
    public Map<String, Object> sendMessage(Integer senderId, Integer receiverId, String message) {

        if (message == null || message.trim().isEmpty() || message.length() > 2000) {
            throw new IllegalArgumentException("Message must be 1-2000 characters");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        // Fetch users
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        String sanitizedMessage = XSS_POLICY.sanitize(message.trim());

        Message msg = new Message();
        msg.setSender(sender);        //  set entity
        msg.setReceiver(receiver);    //  set entity
        msg.setMessage(sanitizedMessage);

        Message saved = messageRepository.save(msg);

        logger.info("Message sent from {} to {}", senderId, receiverId);

        return Map.of(
                "message", "Message sent",
                "id", saved.getId()
        );
    }

    public List<Map<String, Object>> getConversation(Integer currentUserId, Integer otherUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User otherUser = userRepository.findById(otherUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Message> messages = messageRepository.findConversation(currentUser, otherUser);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message m : messages) {
            if (Boolean.TRUE.equals(m.getDeletedForEveryone())) continue;

            Integer senderId = m.getSender().getUserId();
            Integer receiverId = m.getReceiver().getUserId();

            if ((senderId.equals(currentUserId) && Boolean.TRUE.equals(m.getDeletedBySender())) ||
                (receiverId.equals(currentUserId) && Boolean.TRUE.equals(m.getDeletedByReceiver()))) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("senderId", senderId);
            map.put("receiverId", receiverId);
            map.put("message", m.getMessage());
            map.put("isRead", m.getIsRead());
            map.put("createdAt", m.getCreatedAt());

            result.add(map);
        }

        return result;
    }


    public void markAsRead(Integer messageId, Integer currentUserId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            // Only mark as read if the current user is the receiver
            if (msg.getReceiver().getUserId().equals(currentUserId)) {
                msg.setIsRead(true);
                messageRepository.save(msg);
                logger.info("Message {} marked as read by user {}", messageId, currentUserId);
            } else {
                logger.warn("User {} attempted to mark message {} as read, but is not the receiver", currentUserId, messageId);
            }
        });
    }

    @Transactional
    public void markConversationAsRead(Integer currentUserId, Integer otherUserId) {
        User currentUser = userRepository.findByUserId(currentUserId);
        User otherUser = userRepository.findByUserId(otherUserId);

        if (currentUser == null || otherUser == null) {
            throw new IllegalArgumentException("Invalid user IDs");
        }

        List<Message> messages = messageRepository.findConversation(currentUser, otherUser);

        for (Message msg : messages) {
            if (msg.getReceiver().equals(currentUser) && !msg.getIsRead()) {
                msg.setIsRead(true);
            }
        }

        messageRepository.saveAll(messages);
    }


    public Long getUnreadCount(Integer userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return messageRepository.countByReceiverAndIsReadFalseAndDeletedByReceiverFalseAndDeletedForEveryoneFalse(user);
    }

    public void deleteMessage(Integer messageId, Integer userId, String deleteType) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            // User can delete messages they sent OR received
            if (!msg.getSender().getUserId().equals(userId) && !msg.getReceiver().getUserId().equals(userId)) {
                throw new IllegalArgumentException("You can only delete messages you sent or received");
            }

            if ("FOR_EVERYONE".equals(deleteType)) {
                // Only sender can delete for everyone
                if (!msg.getSender().getUserId().equals(userId)) {
                    throw new IllegalArgumentException("You can only delete for everyone if you sent the message");
                }
                msg.setDeletedForEveryone(true);
                messageRepository.save(msg);
                logger.info("Message {} deleted for everyone by user {}", messageId, userId);
            } else {
                // Delete for me only - both sender and receiver can do this
                if (msg.getSender().getUserId().equals(userId)) {
                    msg.setDeletedBySender(true);
                } else if (msg.getReceiver().getUserId().equals(userId)) {
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
            if (announcement.getCreatedBy().getUserId().equals(adminId)) {
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
        List<AnnouncementRead> reads = announcementReadRepository.findByAnnouncementIdWithUser(announcementId);

        List<Map<String, Object>> readBy = new ArrayList<>();
        for (AnnouncementRead read : reads) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", read.getUser().getUserId());
            map.put("username", read.getUser().getUsername());
            map.put("readAt", read.getReadAt());
            readBy.add(map);
        }

        return Map.of(
            "announcementId", announcementId,
            "totalReads", reads.size(),
            "readBy", readBy
        );
    }

}
