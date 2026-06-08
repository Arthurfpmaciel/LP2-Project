package com.agentmanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agentmanager.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByApiKeyId(Long apiKeyId);

    List<Conversation> findByAgentId(Long agentId);
}
