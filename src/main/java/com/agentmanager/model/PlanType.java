package com.agentmanager.model;

public enum PlanType {
    FREE(10_000L),
    PRO(50_000L),
    MASTER(100_000L);

    private final long dailyTokenLimit;

    PlanType(long dailyTokenLimit) {
        this.dailyTokenLimit = dailyTokenLimit;
    }

    public long getDailyTokenLimit() {
        return dailyTokenLimit;
    }

    public boolean canAccess(PlanType requiredLevel) {
        return this.ordinal() >= requiredLevel.ordinal();
    }
}
