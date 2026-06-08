package com.agentmanager.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agentmanager.dto.ApiKeyRequest;
import com.agentmanager.dto.ApiKeyResponse;
import com.agentmanager.service.ApiKeyService;

@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<ApiKeyResponse> list(@RequestParam(required = false) Long userId) {
        return apiKeyService.list(userId);
    }

    @GetMapping("/{id}")
    public ApiKeyResponse findById(@PathVariable Long id) {
        return apiKeyService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> create(@Valid @RequestBody ApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.create(request);
        return ResponseEntity.created(URI.create("/api/api-keys/" + response.id())).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
