package com.agentmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentmanager.model.Agent;
import com.agentmanager.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByApiKeyId(Long apiKeyId);

    List<Conversation> findByAgentId(Long agentId);

    @Query("""
            select coalesce(sum(c.inputTokens), 0)
            from Conversation c
            where c.apiKey.user.id = :userId
            """)
    Long getTotalInputTokensByUserId(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(c.outputTokens), 0)
            from Conversation c
            where c.apiKey.user.id = :userId
            """)
    Long getTotalOutputTokensByUserId(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(c.inputTokens), 0)
            from Conversation c
            where c.apiKey.id = :apiKeyId
            """)
    Long getTotalInputTokensByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    @Query("""
            select coalesce(sum(c.outputTokens), 0)
            from Conversation c
            where c.apiKey.id = :apiKeyId
            """)
    Long getTotalOutputTokensByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    @Query("""
            select coalesce(sum(c.inputTokens + c.outputTokens), 0)
            from Conversation c
            where c.apiKey.user.id = :userId
              and c.createdAt >= :startOfDay
              and c.createdAt < :startOfNextDay
            """)
            
    Long getDailyTokenTotalByUserId(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );

    @Modifying
    @Query("""
            update Conversation c
            set c.agent = :targetAgent
            where c.agent = :sourceAgent
            """)
    int reassignAgent(
            @Param("sourceAgent") Agent sourceAgent,
            @Param("targetAgent") Agent targetAgent
    );
}
