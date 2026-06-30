package com.agentmanager.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agentmanager.dto.AgentResponse;
import com.agentmanager.model.PlanType;
import com.agentmanager.service.AgentService;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public List<AgentResponse> list(@RequestParam(required = false) PlanType level) {
        return agentService.list(level);
    }

    @GetMapping("/{id}")
    public AgentResponse findById(@PathVariable Long id) {
        return agentService.findById(id);
    }

    @GetMapping("/levels/{level}")
    public AgentResponse findByLevel(@PathVariable PlanType level) {
        return agentService.findByLevel(level);
    }
}
