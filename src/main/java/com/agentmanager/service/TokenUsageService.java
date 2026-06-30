package com.agentmanager.service;

import com.agentmanager.dto.ConversationTokensByApiKeyResponse;
import com.agentmanager.dto.ConversationTokensResponse;
import com.agentmanager.dto.DailyTokenUsageResponse;
import com.agentmanager.model.User;
import com.agentmanager.repository.ConversationRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class TokenUsageService {

    private final ConversationRepository conversationRepository;
    private final ApiKeyService apiKeyService;
    private final UserService userService;

    public TokenUsageService(
            ConversationRepository conversationRepository,
            ApiKeyService apiKeyService,
            UserService userService
    ) {
        this.conversationRepository = conversationRepository;
        this.apiKeyService = apiKeyService;
        this.userService = userService;
    }

    public ConversationTokensResponse getTotalTokensByUser(Long userId) {
        userService.getEntity(userId);
        Object[] result = conversationRepository.getTokenTotalsByUserId(userId);
        Long totalInputTokens = ((Number) result[0]).longValue();
        Long totalOutputTokens = ((Number) result[1]).longValue();
        return new ConversationTokensResponse(
                userId,
                totalInputTokens,
                totalOutputTokens,
                totalInputTokens + totalOutputTokens
        );
    }

    public ConversationTokensByApiKeyResponse getTotalTokensByApiKey(Long apiKeyId) {
        apiKeyService.getEntity(apiKeyId);
        Object[] result = conversationRepository.getTokenTotalsByApiKeyId(apiKeyId);
        Long totalInputTokens = ((Number) result[0]).longValue();
        Long totalOutputTokens = ((Number) result[1]).longValue();

        return new ConversationTokensByApiKeyResponse(
                apiKeyId,
                totalInputTokens,
                totalOutputTokens,
                totalInputTokens + totalOutputTokens
        );
    }

    public DailyTokenUsageResponse getDailyTokenUsage(Long userId) {
        User user = userService.getEntity(userId);
        long consumedTokens = getDailyTokenTotal(userId);
        long dailyLimit = user.getPlanType().getDailyTokenLimit();

        double consumptionPercentage = dailyLimit == 0 ? 0 : (consumedTokens * 100.0) / dailyLimit;
        return new DailyTokenUsageResponse(
                userId,
                user.getPlanType(),
                consumedTokens,
                dailyLimit,
                consumptionPercentage
        );
    }

    public long getDailyTokenTotal(Long userId) {
        LocalDate today = LocalDate.now();
        return conversationRepository.getDailyTokenTotalByUserId(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }
}
