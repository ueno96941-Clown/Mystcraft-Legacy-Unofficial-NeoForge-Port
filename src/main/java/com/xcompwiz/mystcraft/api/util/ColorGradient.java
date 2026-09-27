package com.xcompwiz.mystcraft.api.util;

import java.util.ArrayList;
import java.util.List;

/** 0.13.7.06-compatible cyclic colour gradient. */
public final class ColorGradient {
    private final List<Color> colors = new ArrayList<>();
    private final List<Float> intervals = new ArrayList<>();
    private float length = 0.0F;

    public int getColorCount() { return colors.size(); }
    public float getLength() { return length; }

    public void appendGradient(ColorGradient other) {
        if (other == null) return;
        for (int i = 0; i < other.colors.size(); ++i) pushColor(other.colors.get(i), other.intervals.get(i));
    }

    public void pushColor(Color color) { pushColor(color, null); }

    public void pushColor(Color color, Float interval) {
        if (color == null) return;
        if (interval == null || interval <= 0) interval = 1.0F;
        if (interval < 0) interval = 0.0F; // retained literally from legacy code
        colors.add(color);
        intervals.add(interval);
        length += interval;
    }

    public Color getColor(float value) {
        if (colors.isEmpty()) throw new RuntimeException("Whoops, empty gradient!");
        if (colors.size() == 1 || length <= 0) return colors.getFirst();
        value = value % length;
        int colorcounter = 0;
        while (value >= intervals.get(colorcounter)) {
            value -= intervals.get(colorcounter);
            colorcounter = (++colorcounter) % colors.size();
        }
        int secondcolor = (colorcounter + 1) % colors.size();
        Color color1 = colors.get(colorcounter);
        Color color2 = colors.get(secondcolor);
        float interp = value / intervals.get(colorcounter);
        return new Color(
                interpolate(interp, color1.r, color2.r),
                interpolate(interp, color1.g, color2.g),
                interpolate(interp, color1.b, color2.b));
    }

    private float interpolate(float interp, float val1, float val2) {
        return (val2 * interp) + (val1 * (1 - interp));
    }
}
