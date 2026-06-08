package com.agentmanager.dto;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        Long id,
        Long userId,
        String value,
        LocalDateTime createdAt
) {
}
