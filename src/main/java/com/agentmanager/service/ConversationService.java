package com.agentmanager.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentmanager.dto.ConversationTokensByApiKeyResponse;
import com.agentmanager.dto.ConversationRequest;
import com.agentmanager.dto.ConversationResponse;
import com.agentmanager.dto.ConversationTokensResponse;
import com.agentmanager.dto.DailyTokenUsageResponse;
import com.agentmanager.dto.LlmResponse;
import com.agentmanager.exception.BusinessException;
import com.agentmanager.exception.ResourceNotFoundException;
import com.agentmanager.exception.TokenLimitExceededException;
import com.agentmanager.model.Agent;
import com.agentmanager.model.ApiKey;
import com.agentmanager.model.Conversation;
import com.agentmanager.model.User;
import com.agentmanager.repository.ConversationRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ApiKeyService apiKeyService;
    private final AgentService agentService;
    private final UserService userService;
    private final GroqService groqService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ApiKeyService apiKeyService,
            AgentService agentService,
            UserService userService,
            GroqService groqService
    ) {
        this.conversationRepository = conversationRepository;
        this.apiKeyService = apiKeyService;
        this.agentService = agentService;
        this.userService = userService;
        this.groqService = groqService;
    }

    public List<ConversationResponse> list(Long apiKeyId, Long agentId) {
        if (apiKeyId != null) {
            return conversationRepository.findByApiKeyId(apiKeyId).stream().map(this::toResponse).toList();
        }
        if (agentId != null) {
            return conversationRepository.findByAgentId(agentId).stream().map(this::toResponse).toList();
        }
        return conversationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ConversationResponse findById(Long id) {
        return toResponse(getEntity(id));
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

    @Transactional
    public ConversationResponse create(ConversationRequest request) {
        ApiKey apiKey = apiKeyService.getEntity(request.apiKeyId());
        Agent agent = agentService.getEntity(request.agentId());

        if (!apiKey.getUser().getPlanType().canAccess(agent.getLevel())) {
            throw new BusinessException("O plano do usuario nao permite acessar este agente.");
        }

        validateDailyTokenLimit(apiKey.getUser(), request.inputTokens(), request.outputTokens());

        Conversation conversation = new Conversation();
        conversation.setApiKey(apiKey);
        conversation.setAgent(agent);
        conversation.setInput(request.input());
        conversation.setOutput(request.output());
        conversation.setInputTokens(request.inputTokens());
        conversation.setOutputTokens(request.outputTokens());
        conversation.setLatencyMs(request.latencyMs());

        return toResponse(conversationRepository.save(conversation));
    }

    private void validateDailyTokenLimit(User user, int inputTokens, int outputTokens) {
        LocalDate today = LocalDate.now();
        long usedToday = conversationRepository.getDailyTokenTotalByUserId(
                user.getId(),
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        long requestedTokens = (long) inputTokens + outputTokens;
        long dailyLimit = user.getPlanType().getDailyTokenLimit();

        if (usedToday + requestedTokens > dailyLimit) {
            long remainingTokens = Math.max(0, dailyLimit - usedToday);
            throw new TokenLimitExceededException(
                    "Limite diario de tokens excedido. Plano: " + user.getPlanType()
                            + ", limite: " + dailyLimit
                            + ", disponiveis hoje: " + remainingTokens + "."
            );
        }
    }

    public DailyTokenUsageResponse getDailyTokenUsage(Long userId) {
        User user = userService.getEntity(userId);
        LocalDate today = LocalDate.now();
        long consumedTokens = conversationRepository.getDailyTokenTotalByUserId(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
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

    @Transactional
    public void delete(Long id) {
        Conversation conversation = getEntity(id);
        conversationRepository.delete(conversation);
    }

    private Conversation getEntity(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada: " + id));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getApiKey().getId(),
                conversation.getAgent().getId(),
                conversation.getInput(),
                conversation.getOutput(),
                conversation.getInputTokens(),
                conversation.getOutputTokens(),
                conversation.getLatencyMs(),
                conversation.getCreatedAt()
        );
    }
    @Transactional
    public LlmResponse askLlm(Long agentId, String apiKeyValue, String input) {
        ApiKey apiKey = apiKeyService.getEntityByValue(apiKeyValue);
        User user = apiKey.getUser();
        Agent agent = agentService.getEntity(agentId);

        if (!user.getPlanType().canAccess(agent.getLevel())){
            throw new BusinessException("O plano no usuário não permite acesso a este agente.");
        }

        LlmResponse llmResponse = groqService.complete("você é um assistente que ajuda a responder perguntas de usuários, responda de forma clara e objetiva.",input);

        // TODO: validar se o usuario ainda tem tokens disponíveis antes de enviar para a API

        validateDailyTokenLimit(user, llmResponse.inputTokens(), llmResponse.outputTokens());
        Conversation conversation = new Conversation();
        conversation.setAgent(agent);
        conversation.setApiKey(apiKey);
        conversation.setInput(input);
        conversation.setInputTokens(llmResponse.inputTokens());
        conversation.setOutputTokens(llmResponse.outputTokens());
        conversation.setLatencyMs(llmResponse.latencyMs());
        conversation.setOutput(llmResponse.content());

        conversationRepository.save(conversation);
        return llmResponse;
    }

}
