package com.agentmanager.config;

import com.agentmanager.model.Agent;
import com.agentmanager.model.PlanType;
import com.agentmanager.repository.AgentRepository;
import com.agentmanager.repository.ConversationRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AgentCatalogInitializer implements ApplicationRunner {

    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;

    public AgentCatalogInitializer(
            AgentRepository agentRepository,
            ConversationRepository conversationRepository
    ) {
        this.agentRepository = agentRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PlanType level : PlanType.values()) {
            reconcileLevel(level);
        }
    }

    private void reconcileLevel(PlanType level) {
        List<Agent> agents = agentRepository.findByLevel(level).stream()
                .sorted(Comparator.comparing(Agent::getId))
                .toList();

        Agent canonical = agents.isEmpty() ? createAgent(level) : agents.get(0);
        applyCatalogData(canonical, level);
        agentRepository.save(canonical);

        agents.stream()
                .skip(1)
                .forEach(duplicate -> {
                    conversationRepository.reassignAgent(duplicate, canonical);
                    agentRepository.delete(duplicate);
                });
    }

    private Agent createAgent(PlanType level) {
        Agent agent = new Agent();
        agent.setLevel(level);
        return agent;
    }

    private void applyCatalogData(Agent agent, PlanType level) {
        agent.setName(nameFor(level));
        agent.setDescription(descriptionFor(level));
        agent.setLevel(level);
    }

    private String nameFor(PlanType level) {
        return switch (level) {
            case FREE -> "Agente Free";
            case PRO -> "Agente PRO";
            case MASTER -> "Agente Master";
        };
    }

    private String descriptionFor(PlanType level) {
        return switch (level) {
            case FREE -> "Responde perguntas gerais com filtro de seguranca.";
            case PRO -> "Inclui as funcionalidades do Free, consulta o site do IMD via Tavily e informa consumo de tokens.";
            case MASTER -> "Inclui as funcionalidades do PRO e usa RAG com a base local de conhecimento do IMD.";
        };
    }
}
