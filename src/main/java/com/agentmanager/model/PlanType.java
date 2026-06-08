package com.agentmanager.model;

public enum PlanType {
    FREE,
    PLUS,
    MASTER;

    public boolean canAccess(PlanType requiredLevel) {
        return this.ordinal() >= requiredLevel.ordinal();
    }
}
