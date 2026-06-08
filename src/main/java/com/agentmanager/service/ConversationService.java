package com.agentmanager.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentmanager.dto.ConversationRequest;
import com.agentmanager.dto.ConversationResponse;
import com.agentmanager.exception.BusinessException;
import com.agentmanager.exception.ResourceNotFoundException;
import com.agentmanager.model.Agent;
import com.agentmanager.model.ApiKey;
import com.agentmanager.model.Conversation;
import com.agentmanager.repository.ConversationRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ApiKeyService apiKeyService;
    private final AgentService agentService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ApiKeyService apiKeyService,
            AgentService agentService
    ) {
        this.conversationRepository = conversationRepository;
        this.apiKeyService = apiKeyService;
        this.agentService = agentService;
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

    @Transactional
    public ConversationResponse create(ConversationRequest request) {
        ApiKey apiKey = apiKeyService.getEntity(request.apiKeyId());
        Agent agent = agentService.getEntity(request.agentId());

        if (!apiKey.getUser().getPlanType().canAccess(agent.getLevel())) {
            throw new BusinessException("O plano do usuario nao permite acessar este agente.");
        }

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
}
