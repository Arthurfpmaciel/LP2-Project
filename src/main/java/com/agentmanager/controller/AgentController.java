package com.agentmanager.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agentmanager.dto.AgentRequest;
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

    @PostMapping
    public ResponseEntity<AgentResponse> create(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.create(request);
        return ResponseEntity.created(URI.create("/api/agents/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable Long id, @Valid @RequestBody AgentRequest request) {
        return agentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
