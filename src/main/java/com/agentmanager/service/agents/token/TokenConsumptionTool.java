package com.agentmanager.service.agents.token;

import com.agentmanager.dto.ConversationTokensByApiKeyResponse;
import com.agentmanager.dto.ConversationTokensResponse;
import com.agentmanager.dto.DailyTokenUsageResponse;
import com.agentmanager.exception.BusinessException;
import com.agentmanager.service.TokenUsageService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class TokenConsumptionTool {

    private final TokenUsageService tokenUsageService;

    public TokenConsumptionTool(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @Tool("Consulta o consumo diario de tokens do proprio usuario autenticado.")
    public String consultarConsumoDiario(
            @P("ID do usuario autenticado") Long userId
    ) {
        if (userId == null) {
            throw new BusinessException("Usuario nao informado para consulta de consumo.");
        }
        DailyTokenUsageResponse usage = tokenUsageService.getDailyTokenUsage(userId);
        return """
                Consumo diario do usuario %d:
                - Plano: %s
                - Tokens consumidos hoje: %d
                - Limite diario: %d
                - Percentual usado: %.2f%%
                """.formatted(
                usage.userId(),
                usage.planType(),
                usage.consumedTokens(),
                usage.dailyTokenLimit(),
                usage.consumptionPercentage()
        );
    }

    @Tool("Consulta o consumo total de tokens do proprio usuario autenticado.")
    public String consultarConsumoTotalUsuario(
            @P("ID do usuario autenticado") Long userId
    ) {
        if (userId == null) {
            throw new BusinessException("Usuario nao informado para consulta de consumo.");
        }
        ConversationTokensResponse usage = tokenUsageService.getTotalTokensByUser(userId);
        return """
                Consumo total do usuario %d:
                - Tokens de entrada: %d
                - Tokens de saida: %d
                - Total: %d
                """.formatted(
                usage.userId(),
                usage.totalInputTokens(),
                usage.totalOutputTokens(),
                usage.totalTokens()
        );
    }

    @Tool("Consulta o consumo total de tokens da propria chave de API autenticada.")
    public String consultarConsumoTotalApiKey(
            @P("ID da chave de API autenticada") Long apiKeyId
    ) {
        if (apiKeyId == null) {
            throw new BusinessException("Chave de API nao informada para consulta de consumo.");
        }
        ConversationTokensByApiKeyResponse usage = tokenUsageService.getTotalTokensByApiKey(apiKeyId);
        return """
                Consumo total da chave de API %d:
                - Tokens de entrada: %d
                - Tokens de saida: %d
                - Total: %d
                """.formatted(
                usage.apiKeyId(),
                usage.totalInputTokens(),
                usage.totalOutputTokens(),
                usage.totalTokens()
        );
    }
}
