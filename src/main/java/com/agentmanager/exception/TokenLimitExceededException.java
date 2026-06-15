package com.agentmanager.exception;

public class TokenLimitExceededException extends RuntimeException {
    public TokenLimitExceededException(String message) {
        super(message);
    }
}
