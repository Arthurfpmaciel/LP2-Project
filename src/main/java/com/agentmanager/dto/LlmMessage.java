package com.agentmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record LlmMessage(
        @NotBlank String role,
        @NotBlank String content
) {
}
