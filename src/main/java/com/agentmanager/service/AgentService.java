package com.agentmanager.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentmanager.dto.AgentRequest;
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
        return agentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AgentResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public AgentResponse create(AgentRequest request) {
        Agent agent = new Agent();
        applyRequest(agent, request);
        return toResponse(agentRepository.save(agent));
    }

    @Transactional
    public AgentResponse update(Long id, AgentRequest request) {
        Agent agent = getEntity(id);
        applyRequest(agent, request);
        return toResponse(agentRepository.save(agent));
    }

    @Transactional
    public void delete(Long id) {
        Agent agent = getEntity(id);
        agentRepository.delete(agent);
    }

    public Agent getEntity(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agente nao encontrado: " + id));
    }

    private void applyRequest(Agent agent, AgentRequest request) {
        agent.setName(request.name());
        agent.setDescription(request.description());
        agent.setLevel(request.level());
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
