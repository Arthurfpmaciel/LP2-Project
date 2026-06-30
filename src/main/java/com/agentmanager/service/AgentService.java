package com.agentmanager.service;

import java.util.List;
import java.util.Comparator;
import org.springframework.stereotype.Service;

import com.agentmanager.dto.AgentResponse;
import com.agentmanager.exception.ResourceNotFoundException;
import com.agentmanager.model.Agent;
import com.agentmanager.model.PlanType;
import com.agentmanager.repository.AgentRepository;

@Service
public class AgentService {
    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<AgentResponse> list(PlanType level) {
        if (level != null) {
            return agentRepository.findByLevel(level).stream().map(this::toResponse).toList();
        }
        return agentRepository.findAll().stream()
                .sorted(Comparator.comparing(agent -> agent.getLevel().ordinal()))
                .map(this::toResponse)
                .toList();
    }

    public AgentResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public AgentResponse findByLevel(PlanType level) {
        return toResponse(getEntityByLevel(level));
    }

    public Agent getEntity(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + id));
    }

    public Agent getEntityByLevel(PlanType level) {
        return agentRepository.findFirstByLevelOrderByIdAsc(level)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + level));
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getLevel(),
                agent.getCreatedAt()
        );
    }
}
