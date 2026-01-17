package com.example.visited.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.visited.entitys.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    
    @Query("SELECT m FROM Message m WHERE (m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1) ORDER BY m.createdAt ASC")
    List<Message> findConversation(Integer userId1, Integer userId2);
    
    Long countByReceiverIdAndIsReadFalseAndDeletedByReceiverFalseAndDeletedForEveryoneFalse(Integer receiverId);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiverId = ?1 AND m.senderId = ?2 AND m.isRead = false")
    int markConversationAsReadBatch(Integer receiverId, Integer senderId);
}
