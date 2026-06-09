package com.agentmanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentmanager.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByApiKeyId(Long apiKeyId);

    List<Conversation> findByAgentId(Long agentId);

    @Query("""
            select
                coalesce(sum(c.inputTokens), 0),
                coalesce(sum(c.outputTokens), 0)
            from Conversation c
            where c.apiKey.user.id = :userId
            """)
    Object[] getTokenTotalsByUserId(@Param("userId") Long userId);

    @Query("""
            select
                coalesce(sum(c.inputTokens), 0),
                coalesce(sum(c.outputTokens), 0)
            from Conversation c
            where c.apiKey.id = :apiKeyId
            """)
    Object[] getTokenTotalsByApiKeyId(@Param("apiKeyId") Long apiKeyId);
}
