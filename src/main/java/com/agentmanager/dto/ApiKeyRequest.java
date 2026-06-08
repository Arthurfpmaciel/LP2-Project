package com.agentmanager.dto;

import jakarta.validation.constraints.NotNull;

public record ApiKeyRequest(
        @NotNull Long userId
) {
}
