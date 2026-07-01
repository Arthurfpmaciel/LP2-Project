package com.agentmanager.service.agents;

import com.agentmanager.dto.LlmResponse;
import com.agentmanager.exception.UpstreamLlmException;
import com.agentmanager.model.PlanType;
import com.agentmanager.service.agents.rag.KnowledgeChunk;
import com.agentmanager.service.agents.rag.LocalKnowledgeBase;
import com.agentmanager.service.agents.tavily.TavilySearchResult;
import com.agentmanager.service.agents.tavily.TavilySearchService;
import com.agentmanager.service.agents.token.TokenConsumptionTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AgentGraphService {

    private final RouterNode routerNode;
    private final GenericNode genericNode;
    private final EvidenceAnswerNode evidenceAnswerNode;
    private final TokenUsageNode tokenUsageNode;
    private final RefinementNode refinementNode;
    private final TavilySearchService tavilySearchService;
    private final LocalKnowledgeBase localKnowledgeBase;
    private final AgentResponseBuilder responseBuilder;

    public AgentGraphService(
            ChatModel chatModel,
            TokenConsumptionTool tokenConsumptionTool,
            TavilySearchService tavilySearchService,
            LocalKnowledgeBase localKnowledgeBase,
            AgentResponseBuilder responseBuilder
    ) {
        this.routerNode = AiServices.builder(RouterNode.class)
                .chatModel(chatModel)
                .build();
        this.genericNode = AiServices.builder(GenericNode.class)
                .chatModel(chatModel)
                .build();
        this.evidenceAnswerNode = AiServices.builder(EvidenceAnswerNode.class)
                .chatModel(chatModel)
                .build();
        this.tokenUsageNode = AiServices.builder(TokenUsageNode.class)
                .chatModel(chatModel)
                .tools(tokenConsumptionTool)
                .build();
        this.refinementNode = AiServices.builder(RefinementNode.class)
                .chatModel(chatModel)
                .build();
        this.tavilySearchService = tavilySearchService;
        this.localKnowledgeBase = localKnowledgeBase;
        this.responseBuilder = responseBuilder;
    }

    public LlmResponse run(String input) {
        return run(PlanType.FREE, AgentExecutionRequest.fromInput(input)).response();
    }

    public AgentExecutionResult runFree(AgentExecutionRequest request) {
        return run(PlanType.FREE, request);
    }

    public AgentExecutionResult runPro(AgentExecutionRequest request) {
        return run(PlanType.PRO, request);
    }

    public AgentExecutionResult runMaster(AgentExecutionRequest request) {
        return run(PlanType.MASTER, request);
    }

    public AgentExecutionResult run(PlanType agentLevel, AgentExecutionRequest request) {
        try {
            Instant start = Instant.now();
            AgentTokenUsage tokenUsage = AgentTokenUsage.empty();
            String input = request.input();

            Result<String> routeResponse = routerNode.route(input);
            tokenUsage = tokenUsage.plus(AgentTokenUsage.from(routeResponse));
            AgentIntent intent = parseIntent(routeResponse.content());

            if (intent == AgentIntent.INVALID) {
                LlmResponse response = responseBuilder.build(AgentPrompts.SAFETY_REFUSAL, tokenUsage, start);
                return new AgentExecutionResult(agentLevel, intent, response);
            }

            if (agentLevel == PlanType.FREE) {
                LlmResponse response = runFree(input, intent, tokenUsage, start);
                return new AgentExecutionResult(agentLevel, intent, response);
            }

            if (intent == AgentIntent.TOKEN_USAGE) {
                Result<String> response = tokenUsageNode.answer(tokenUsagePrompt(input, request));
                tokenUsage = tokenUsage.plus(AgentTokenUsage.from(response));
                LlmResponse llmResponse = responseBuilder.build(response.content(), tokenUsage, start);
                return new AgentExecutionResult(agentLevel, intent, llmResponse);
            }

            if (agentLevel == PlanType.MASTER) {
                List<KnowledgeChunk> chunks = localKnowledgeBase.retrieve(input, 3);
                if (!chunks.isEmpty() || intent == AgentIntent.LOCAL_KNOWLEDGE) {
                    LlmResponse response = runMasterRag(input, tokenUsage, start, chunks);
                    return new AgentExecutionResult(agentLevel, AgentIntent.LOCAL_KNOWLEDGE, response);
                }
            }

            if (intent == AgentIntent.IMD_SITE || intent == AgentIntent.LOCAL_KNOWLEDGE) {
                LlmResponse response = runProSearch(input, tokenUsage, start);
                return new AgentExecutionResult(agentLevel, intent, response);
            }

            LlmResponse response = runGenericWithRefinement(input, tokenUsage, start);
            return new AgentExecutionResult(agentLevel, intent, response);
        } catch (RuntimeException e) {
            int upstreamStatus = parseUpstreamStatus(e);
            if (upstreamStatus > 0) {
                throw new UpstreamLlmException(upstreamStatus, e.getMessage());
            }
            throw e;
        }
    }

    private static final Pattern UPSTREAM_HTTP_STATUS = Pattern.compile("Erro HTTP (\\d{3})");

    private static int parseUpstreamStatus(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                Matcher m = UPSTREAM_HTTP_STATUS.matcher(msg);
                if (m.find()) {
                    try {
                        return Integer.parseInt(m.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            cur = cur.getCause();
        }
        return -1;
    }

    private LlmResponse runFree(String input, AgentIntent intent, AgentTokenUsage tokenUsage, Instant start) {
        if (intent == AgentIntent.TOKEN_USAGE) {
            String content = "A consulta de consumo de tokens está disponível nos agentes PRO e MASTER.";
            return responseBuilder.build(content, tokenUsage, start);
        }

        Result<String> genericResponse = genericNode.answer(input);
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(genericResponse));
        return responseBuilder.build(cleanThinking(genericResponse.content()), tokenUsage, start);
    }

    private LlmResponse runGenericWithRefinement(String input, AgentTokenUsage tokenUsage, Instant start) {
        Result<String> genericResponse = genericNode.answer(input);
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(genericResponse));

        Result<String> refinedResponse = refinementNode.refine("""
                Pergunta do usuário:
                %s

                Resposta inicial:
                %s
                """.formatted(input, cleanThinking(genericResponse.content())));
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(refinedResponse));

        return responseBuilder.build(cleanThinking(refinedResponse.content()), tokenUsage, start);
    }

    private LlmResponse runProSearch(String input, AgentTokenUsage tokenUsage, Instant start) {
        List<TavilySearchResult> results = tavilySearchService.searchImd(input);
        String evidence = tavilySearchService.formatResults(results);

        Result<String> draft = evidenceAnswerNode.answer(evidencePrompt(input, "Resultados do site do IMD via Tavily", evidence));
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(draft));

        Result<String> refined = refinementNode.refine(refinementPrompt(input, evidence, cleanThinking(draft.content())));
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(refined));

        return responseBuilder.build(cleanThinking(refined.content()), tokenUsage, start);
    }

    private LlmResponse runMasterRag(
            String input,
            AgentTokenUsage tokenUsage,
            Instant start,
            List<KnowledgeChunk> chunks
    ) {
        String evidence = localKnowledgeBase.formatChunks(chunks);

        Result<String> draft = evidenceAnswerNode.answer(evidencePrompt(input, "Base local de conhecimento do IMD", evidence));
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(draft));

        Result<String> refined = refinementNode.refine(refinementPrompt(input, evidence, cleanThinking(draft.content())));
        tokenUsage = tokenUsage.plus(AgentTokenUsage.from(refined));

        return responseBuilder.build(cleanThinking(refined.content()), tokenUsage, start);
    }

    private AgentIntent parseIntent(String content) {
        String normalized = cleanThinking(content).trim().toUpperCase();
        for (String line : normalized.split("\\R")) {
            String candidate = line.trim().replaceAll("[^A-Z_]", "");
            for (AgentIntent intent : AgentIntent.values()) {
                if (candidate.equals(intent.name())) {
                    return intent;
                }
            }
        }

        AgentIntent bestIntent = AgentIntent.GENERIC;
        int bestIndex = -1;
        for (AgentIntent intent : AgentIntent.values()) {
            int index = normalized.lastIndexOf(intent.name());
            if (index > bestIndex) {
                bestIntent = intent;
                bestIndex = index;
            }
        }
        return bestIntent;
    }

    private String cleanThinking(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("(?s)<think>.*?</think>\\s*", "").trim();
    }

    private String tokenUsagePrompt(String input, AgentExecutionRequest request) {
        return """
                Mensagem do usuario:
                %s

                Contexto autenticado:
                - userId: %s
                - apiKeyId: %s

                Use as ferramentas disponiveis para consultar apenas os dados desse contexto autenticado.
                Se faltar userId ou apiKeyId para o tipo de consulta pedido, informe que a integracao precisa
                fornecer esse identificador.
                """.formatted(input, request.userId(), request.apiKeyId());
    }

    private String evidencePrompt(String input, String evidenceName, String evidence) {
        return """
                Pergunta do usuario:
                %s

                Evidencias (%s):
                %s

                Responda usando as evidencias. Inclua links quando a evidencia trouxer URL.
                """.formatted(input, evidenceName, evidence);
    }

    private String refinementPrompt(String input, String evidence, String draft) {
        return """
                Pergunta do usuario:
                %s

                Evidencias:
                %s

                Resposta inicial:
                %s
                """.formatted(input, evidence, draft);
    }

    private interface RouterNode {

        @SystemMessage(AgentPrompts.ROUTER_SYSTEM)
        Result<String> route(@UserMessage String input);
    }

    private interface GenericNode {

        @SystemMessage(AgentPrompts.FREE_SYSTEM)
        Result<String> answer(@UserMessage String input);
    }

    private interface EvidenceAnswerNode {

        @SystemMessage("""
                Você é um agente especializado no IMD.
                Responda em português com base no contexto fornecido pelo usuário.
                Não invente dados fora das evidências.
                """)
        Result<String> answer(@UserMessage String prompt);
    }

    private interface TokenUsageNode {

        @SystemMessage("""
                Você ajuda alunos e professores do IMD a consultar o proprio consumo de tokens.
                Use obrigatoriamente uma ferramenta de consumo quando houver identificador suficiente.
                Nunca consulte dados de outro usuario ou chave que nao esteja no contexto autenticado.
                """)
        Result<String> answer(@UserMessage String prompt);
    }

    private interface RefinementNode {

        @SystemMessage(AgentPrompts.REFINER_SYSTEM)
        Result<String> refine(@UserMessage String prompt);
    }
}
