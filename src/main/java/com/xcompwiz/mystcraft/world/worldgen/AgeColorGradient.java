package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;

public record AgeColorGradient(List<AgeGradientPoint> points) {
    public AgeColorGradient {
        points = List.copyOf(points);
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public AgeColorGradient append(AgeColorGradient other) {
        ArrayList<AgeGradientPoint> out = new ArrayList<>(points);
        out.addAll(other.points());
        return new AgeColorGradient(out);
    }

    public AgeColor sample(float value) {
        if (points.isEmpty()) throw new IllegalStateException("empty gradient");
        if (points.size() == 1) return points.get(0).color();

        float length = 0.0F;
        for (AgeGradientPoint p : points) length += p.interval();
        if (length <= 0.0F) return points.get(0).color();

        value = value % length;
        if (value < 0.0F) value += length;

        int index = 0;
        while (value >= points.get(index).interval()) {
            value -= points.get(index).interval();
            index = (index + 1) % points.size();
        }

        AgeGradientPoint first = points.get(index);
        AgeGradientPoint second = points.get((index + 1) % points.size());
        float t = value / first.interval();

        return new AgeColor(
                second.color().r() * t + first.color().r() * (1.0F - t),
                second.color().g() * t + first.color().g() * (1.0F - t),
                second.color().b() * t + first.color().b() * (1.0F - t));
    }
}
