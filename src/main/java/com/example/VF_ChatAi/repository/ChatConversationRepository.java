package com.example.VF_ChatAi.repository;

import com.example.VF_ChatAi.model.ChatConversation;
import com.example.VF_ChatAi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findByUserOrderByUpdatedAtDesc(User user);


    Optional<ChatConversation> findByIdAndUser(Long id, User user);

    @Query("SELECT c FROM ChatConversation c WHERE c.user = :user AND c.id = :id")
    Optional<ChatConversation> findByIdAndUserWithMessages(@Param("id") Long id, @Param("user") User user);

    long countByUser(User user);
}