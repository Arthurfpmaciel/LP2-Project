package com.agentmanager.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${llm.provider}") String provider,
            @Value("${groq.base-url}") String groqBaseUrl,
            @Value("${groq.api-key:}") String groqApiKey,
            @Value("${groq.model}") String groqModel,
            @Value("${minimax.base-url}") String minimaxBaseUrl,
            @Value("${minimax.api-key:}") String minimaxApiKey,
            @Value("${minimax.model}") String minimaxModel
    ) {
        String baseUrl;
        String apiKey;
        String model;
        if ("minimax".equalsIgnoreCase(provider)) {
            baseUrl = minimaxBaseUrl;
            apiKey = minimaxApiKey;
            model = minimaxModel;
        } else {
            baseUrl = groqBaseUrl;
            apiKey = groqApiKey;
            model = groqModel;
        }

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }
}
