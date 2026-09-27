package com.xcompwiz.mystcraft.api.instability;

import com.xcompwiz.mystcraft.api.world.logic.IEnvironmentalEffect;

/** API-v1 instability director retained for legacy compatibility; CP299 built-in runtime effects are live. */
public interface InstabilityDirector {
    int getInstabilityScore();
    void registerEffect(IEnvironmentalEffect effect);
}
