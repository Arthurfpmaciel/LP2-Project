package com.agentmanager.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agentmanager.dto.ConversationRequest;
import com.agentmanager.dto.ConversationResponse;
import com.agentmanager.dto.ConversationTokensByApiKeyResponse;
import com.agentmanager.dto.ConversationTokensResponse;
import com.agentmanager.dto.DailyTokenUsageResponse;
import com.agentmanager.dto.InnerLLMPromptRequest;
import com.agentmanager.dto.LlmResponse;
import com.agentmanager.service.ConversationService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationResponse> list(
            @RequestParam(required = false) Long apiKeyId,
            @RequestParam(required = false) Long agentId
    ) {
        return conversationService.list(apiKeyId, agentId);
    }

    @GetMapping("/{id}")
    public ConversationResponse findById(@PathVariable Long id) {
        return conversationService.findById(id);
    }

    @GetMapping("/tokens/users/{userId}")
    public ConversationTokensResponse getTotalTokensByUser(@PathVariable Long userId) {
        return conversationService.getTotalTokensByUser(userId);
    }

    @GetMapping("/tokens/api-keys/{apiKeyId}")
    public ConversationTokensByApiKeyResponse getTotalTokensByApiKey(@PathVariable Long apiKeyId) {
        return conversationService.getTotalTokensByApiKey(apiKeyId);
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(@Valid @RequestBody ConversationRequest request) {
        ConversationResponse response = conversationService.create(request);
        return ResponseEntity.created(URI.create("/api/conversations/" + response.id())).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/llm/users/{userId}")
    public LlmResponse askLlm(
        @PathVariable Long userId,
        @Valid @RequestBody InnerLLMPromptRequest request
    ) {
        return conversationService.askLlm(userId, request.input());
    }

    @GetMapping("/tokens/users/{userId}/daily")
    public DailyTokenUsageResponse getDailyTokenUsage(
            @PathVariable Long userId
    ) {
        return conversationService.getDailyTokenUsage(userId);
    }
}
