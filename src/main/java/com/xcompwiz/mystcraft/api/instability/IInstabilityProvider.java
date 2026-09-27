package com.xcompwiz.mystcraft.api.instability;

/**
 * Public API-v1 instability provider contract. The port retains registration/source
 * compatibility, but project policy never schedules these providers at runtime.
 */
public interface IInstabilityProvider {
    void addEffects(InstabilityDirector controller, Integer level);
}
