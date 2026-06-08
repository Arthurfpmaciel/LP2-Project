package com.agentmanager.service;

import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentmanager.dto.ApiKeyRequest;
import com.agentmanager.dto.ApiKeyResponse;
import com.agentmanager.exception.ResourceNotFoundException;
import com.agentmanager.model.ApiKey;
import com.agentmanager.repository.ApiKeyRepository;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserService userService;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, UserService userService) {
        this.apiKeyRepository = apiKeyRepository;
        this.userService = userService;
    }

    public List<ApiKeyResponse> list(Long userId) {
        if (userId != null) {
            return apiKeyRepository.findByUserId(userId).stream().map(this::toResponse).toList();
        }
        return apiKeyRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ApiKeyResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ApiKeyResponse create(ApiKeyRequest request) {
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(userService.getEntity(request.userId()));
        apiKey.setValue(generateValue());
        return toResponse(apiKeyRepository.save(apiKey));
    }

    @Transactional
    public void delete(Long id) {
        ApiKey apiKey = getEntity(id);
        apiKeyRepository.delete(apiKey);
    }

    public ApiKey getEntity(Long id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chave de API nao encontrada: " + id));
    }

    private String generateValue() {
        // byte[] bytes = new byte[32];
        String value;
        do {
            value = "ak_" +UUID.randomUUID();
            // secureRandom.nextBytes(bytes);
            // value = "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (apiKeyRepository.findByValue(value).isPresent());
        return value;
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getUser().getId(),
                apiKey.getValue(),
                apiKey.getCreatedAt()
        );
    }
}
