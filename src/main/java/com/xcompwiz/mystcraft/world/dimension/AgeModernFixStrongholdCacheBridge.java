package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Optional bridge for ModernFix's perf.cache_strongholds runtime state.
 *
 * <p>ModernFix injects a public method named {@code mfix$setStrongholdCachePath}
 * into {@link ChunkGeneratorStructureState}. Its ServerLevel constructor hook calls
 * that method on the vanilla-created structure state. Mystcraft deliberately replaces
 * that state after ServerLevel construction with an Age-local StructureSet selection,
 * so the replacement must receive the same cache path/server pair.</p>
 *
 * <p>This bridge uses reflection so Mystcraft does not acquire a hard dependency on
 * ModernFix. When the mixin is absent, this is a no-op.</p>
 */
final class AgeModernFixStrongholdCacheBridge {
    private static final String METHOD_NAME = "mfix$setStrongholdCachePath";

    private AgeModernFixStrongholdCacheBridge() {}

    static void attachIfPresent(ServerLevel level, ChunkGeneratorStructureState state) {
        MinecraftServer server = level.getServer();
        if (server == null) return;

        final Method method;
        try {
            method = state.getClass().getMethod(METHOD_NAME, Path.class, MinecraftServer.class);
        } catch (NoSuchMethodException absent) {
            return;
        }

        Path dimensionPath = server.storageSource.getDimensionPath(level.dimension());
        try {
            method.invoke(state, dimensionPath, server);
            Mystcraft.LOGGER.debug(
                    "Attached ModernFix stronghold cache context for Age {} at {}",
                    level.dimension().location(), dimensionPath);
        } catch (IllegalAccessException | InvocationTargetException failure) {
            Throwable cause = failure instanceof InvocationTargetException invocation
                    && invocation.getCause() != null
                    ? invocation.getCause()
                    : failure;
            throw new IllegalStateException(
                    "Failed to attach ModernFix stronghold cache context for "
                            + level.dimension().location(),
                    cause);
        }
    }
}
