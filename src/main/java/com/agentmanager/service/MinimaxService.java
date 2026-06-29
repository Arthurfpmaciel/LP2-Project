package com.agentmanager.service;

import com.agentmanager.dto.LlmMessage;
import com.agentmanager.dto.LlmResponse;
import com.agentmanager.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "minimax")
public class MinimaxService implements LlmService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;

    public MinimaxService(
            RestClient.Builder restClientBuilder,
            @Value("${minimax.base-url}") String baseUrl,
            @Value("${minimax.api-key:}") String apiKey,
            @Value("${minimax.model}") String model,
            @Value("${minimax.temperature}") Double temperature,
            @Value("${minimax.max-tokens}") Integer maxTokens
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt) {
        return chat(List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", userPrompt)
        ));
    }

    @Override
    public Integer maxTokensPerRequest() {
        return maxTokens;
    }

    private LlmResponse chat(List<LlmMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Sem chave de API do MiniMax cadastrada.");
        }
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("Mensagem não pode ser vazia");
        }

        Instant start = Instant.now();
        try {
            MinimaxChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(new MinimaxChatRequest(model, messages, temperature, maxTokens))
                    .retrieve()
                    .body(MinimaxChatResponse.class);

            return toLlmResponse(response, Duration.between(start, Instant.now()).toMillis());
        } catch (RestClientResponseException exception) {
            throw new BusinessException("Erro ao chamar o MiniMax: HTTP "
                    + exception.getStatusCode().value() + " - " + exception.getResponseBodyAsString());
        } catch (RuntimeException exception) {
            throw new BusinessException("Erro ao chamar o MiniMax: " + exception.getMessage());
        }
    }

    private LlmResponse toLlmResponse(MinimaxChatResponse response, Long latencyMs) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException("MiniMax retornou uma resposta vazia.");
        }

        MinimaxChoice firstChoice = response.choices().get(0);
        String content = firstChoice.message() == null ? "" : firstChoice.message().content();
        MinimaxUsage usage = response.usage();

        content = cleanThinking(content);

        Integer inputTokens = usage == null ? 0 : usage.promptTokens();
        Integer outputTokens = usage == null ? 0 : usage.completionTokens();
        Integer totalTokens = usage == null ? inputTokens + outputTokens : usage.totalTokens();

        return new LlmResponse(content, inputTokens, outputTokens, totalTokens, latencyMs, response.model());
    }

    private String cleanThinking(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("(?s)<think>.*?</think>\\s*", "").trim();
    }

    private record MinimaxChatRequest(
            String model,
            List<LlmMessage> messages,
            Double temperature,
            @JsonProperty("max_completion_tokens") Integer maxTokens
    ) {
    }

    private record MinimaxChatResponse(
            String model,
            List<MinimaxChoice> choices,
            MinimaxUsage usage
    ) {
    }

    private record MinimaxChoice(
            LlmMessage message
    ) {
    }

    private record MinimaxUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}