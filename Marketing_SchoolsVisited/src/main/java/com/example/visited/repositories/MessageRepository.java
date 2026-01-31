package com.example.visited.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.visited.entitys.Message;
import com.example.visited.entitys.User;

import jakarta.transaction.Transactional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    
	 @Query("SELECT m FROM Message m " +
	           "WHERE (m.sender = :user1 AND m.receiver = :user2) " +
	           "   OR (m.sender = :user2 AND m.receiver = :user1) " +
	           "ORDER BY m.createdAt ASC")
	    List<Message> findConversation(User user1, User user2);
    
    
	 @Modifying
	    @Transactional
	    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender.userId = :senderId AND m.receiver.userId = :receiverId")
	    int markConversationAsReadBatch(Integer senderId, Integer receiverId);
    Long countByReceiverAndIsReadFalseAndDeletedByReceiverFalseAndDeletedForEveryoneFalse(User receiver);

}
