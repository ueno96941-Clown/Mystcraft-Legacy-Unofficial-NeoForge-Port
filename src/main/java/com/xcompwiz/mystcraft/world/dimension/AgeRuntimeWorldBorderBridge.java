package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps runtime-created Mystcraft Ages on the same world-border contract vanilla
 * secondary dimensions receive during {@code MinecraftServer#createLevels}.
 *
 * <p>Vanilla installs a {@link BorderChangeListener.DelegateBorderChangeListener}
 * from the Overworld border into every secondary dimension. Runtime Ages are
 * created after that startup pass, so Mystcraft must install and later remove
 * the equivalent listener itself.</p>
 */
public final class AgeRuntimeWorldBorderBridge {
    private AgeRuntimeWorldBorderBridge() {}

    private static final Map<ServerLevel, Binding> BINDINGS = new IdentityHashMap<>();

    /**
     * Copies the current Overworld border state and begins forwarding future
     * changes into the runtime Age.
     */
    public static synchronized void attach(ServerLevel overworld, ServerLevel ageLevel) {
        Objects.requireNonNull(overworld, "overworld");
        Objects.requireNonNull(ageLevel, "ageLevel");
        if (overworld == ageLevel) {
            throw new IllegalArgumentException("Mystcraft Age border bridge requires a secondary level");
        }

        detach(ageLevel);

        WorldBorder source = overworld.getWorldBorder();
        WorldBorder target = ageLevel.getWorldBorder();

        // Startup dimensions receive current settings because MinecraftServer
        // reapplies the Overworld border after all delegates are registered.
        // Runtime dimensions miss that pass, so copy the current snapshot first.
        target.applySettings(source.createSettings());

        BorderChangeListener listener = new BorderChangeListener.DelegateBorderChangeListener(target);
        source.addListener(listener);
        BINDINGS.put(ageLevel, new Binding(source, listener));
    }

    /** Removes the forwarding listener for one Age. Safe to call repeatedly. */
    public static synchronized void detach(ServerLevel ageLevel) {
        Binding binding = BINDINGS.remove(ageLevel);
        if (binding != null) {
            binding.source().removeListener(binding.listener());
        }
    }

    /** Integrated-server/test safety: no listener may survive a stopped server. */
    public static synchronized void clearAll() {
        for (Binding binding : BINDINGS.values()) {
            binding.source().removeListener(binding.listener());
        }
        BINDINGS.clear();
    }

    static synchronized int bindingCountForValidation() {
        return BINDINGS.size();
    }

    private record Binding(WorldBorder source, BorderChangeListener listener) {}
}
