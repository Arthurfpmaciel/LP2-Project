package com.agentmanager.exception;

public class UpstreamLlmException extends RuntimeException {

    private final int upstreamStatus;

    public UpstreamLlmException(int upstreamStatus, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}