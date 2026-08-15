package com.carpetplayers.ai;

public class ProviderHealth {
    public final String providerName;
    public final boolean enabled;
    public final boolean onCooldown;
    public final int priority;
    public final int failureCount;
    public final long cooldownUntil;
    public final String lastError;

    public ProviderHealth(String providerName, boolean enabled, boolean onCooldown, int priority,
                          int failureCount, long cooldownUntil, String lastError) {
        this.providerName = providerName;
        this.enabled = enabled;
        this.onCooldown = onCooldown;
        this.priority = priority;
        this.failureCount = failureCount;
        this.cooldownUntil = cooldownUntil;
        this.lastError = lastError;
    }
}
