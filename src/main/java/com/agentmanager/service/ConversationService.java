package com.agentmanager.service;

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
import com.agentmanager.model.PlanType;
import com.agentmanager.model.User;
import com.agentmanager.repository.ConversationRepository;
import com.agentmanager.service.agents.AgentExecutionRequest;
import com.agentmanager.service.agents.AgentExecutionResult;
import com.agentmanager.service.agents.AgentGraphService;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ApiKeyService apiKeyService;
    private final AgentService agentService;
    private final LlmService llmService;
    private final TokenUsageService tokenUsageService;
    private final AgentGraphService agentGraphService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ApiKeyService apiKeyService,
            AgentService agentService,
            LlmService llmService,
            TokenUsageService tokenUsageService,
            AgentGraphService agentGraphService
    ) {
        this.conversationRepository = conversationRepository;
        this.apiKeyService = apiKeyService;
        this.agentService = agentService;
        this.llmService = llmService;
        this.tokenUsageService = tokenUsageService;
        this.agentGraphService = agentGraphService;
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
        return tokenUsageService.getTotalTokensByUser(userId);
    }

    public ConversationTokensByApiKeyResponse getTotalTokensByApiKey(Long apiKeyId) {
        return tokenUsageService.getTotalTokensByApiKey(apiKeyId);
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
        long usedToday = tokenUsageService.getDailyTokenTotal(user.getId());
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
        return tokenUsageService.getDailyTokenUsage(userId);
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
    public LlmResponse askLlm(PlanType agentLevel, String apiKeyValue, String input) {
        ApiKey apiKey = apiKeyService.getEntityByValue(apiKeyValue);
        User user = apiKey.getUser();
        Agent agent = agentService.getEntityByLevel(agentLevel);

        if (!user.getPlanType().canAccess(agentLevel)){
            throw new BusinessException("O plano do usuario nao permite acesso ao agente " + agentLevel + ".");
        }

        validateDailyTokenLimit(user, estimateInputTokens(input), llmService.maxTokensPerRequest());

        AgentExecutionResult result = agentGraphService.run(
                agentLevel,
                new AgentExecutionRequest(input, user.getId(), apiKey.getId())
        );
        LlmResponse llmResponse = result.response();

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

    private int estimateInputTokens(String input) {
        if (input == null || input.isBlank()) {
            return 0;
        }
        return (input.length() + 3) / 4;
    }

}
