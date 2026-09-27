package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

public record AgeFeaturePlan(List<AgeFeaturePlanEntry> entries) {
    public AgeFeaturePlan {
        entries = List.copyOf(entries);
    }
}
