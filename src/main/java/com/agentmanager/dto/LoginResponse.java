package com.agentmanager.dto;

public record LoginResponse (
    Long userID,
    String name,
    String email,
    String planType
){}
