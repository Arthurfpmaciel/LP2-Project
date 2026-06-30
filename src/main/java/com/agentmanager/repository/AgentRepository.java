package com.agentmanager.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agentmanager.model.Agent;
import com.agentmanager.model.PlanType;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByLevel(PlanType level);

    Optional<Agent> findFirstByLevelOrderByIdAsc(PlanType level);
}
