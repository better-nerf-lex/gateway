package com.bntech.lexai.gateway.service.integration.billing;

public class SubscriptionStatusResponse {
    private String entityId;
    private String entityType;
    private boolean isActive;

    public SubscriptionStatusResponse() {
    }

    public SubscriptionStatusResponse(String entityId, String entityType, boolean isActive) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.isActive = isActive;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
