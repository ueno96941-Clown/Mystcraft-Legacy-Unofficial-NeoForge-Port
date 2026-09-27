package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Replays SymbolWeatherStorm's extra per-chunk lightning pass from 0.13.7.06. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeStormLightningRuntimeEvents {
    private static final Map<String, Integer> UPDATE_LCG = new ConcurrentHashMap<>();
    private AgeStormLightningRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        var age = AgeManager.resolveByLevel(level.getServer(), level.dimension());
        if (age.isEmpty()) return;
        var prepared = AgeRuntimeDimensionLoader.getPrepared(age.get().ageUid()).orElse(null);
        if (prepared == null || prepared.worldgenPlan().weatherPlan().mode() != AgeWeatherMode.STORM) return;
        if (!level.isRaining() || !level.isThundering()) return;

        String key = level.dimension().location().toString();
        int lcg = UPDATE_LCG.computeIfAbsent(key, ignored -> level.getRandom().nextInt());
        HashSet<Long> visited = new HashSet<>();
        int simulationDistance = level.getServer().getPlayerList().getSimulationDistance();
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dz = -simulationDistance; dz <= simulationDistance; dz++) {
                for (int dx = -simulationDistance; dx <= simulationDistance; dx++) {
                    int cx = center.x + dx, cz = center.z + dz;
                    long packed = ChunkPos.asLong(cx, cz);
                    if (!visited.add(packed)) continue;
                    if (!level.getChunkSource().chunkMap.getDistanceManager().inBlockTickingRange(packed)) continue;
                    lcg = tryStrike(level, cx, cz, lcg);
                }
            }
        }
        for (long packed : level.getForcedChunks()) {
            if (!visited.add(packed)) continue;
            ChunkPos p = new ChunkPos(packed);
            lcg = tryStrike(level, p.x, p.z, lcg);
        }
        UPDATE_LCG.put(key, lcg);
    }

    static int tryStrike(ServerLevel level, int chunkX, int chunkZ, int updateLCG) {
        // Legacy gate used world.rand.nextInt(100000) before advancing the controller LCG.
        if (level.getRandom().nextInt(100000) != 0) return updateLCG;
        updateLCG = updateLCG * 3 + 1013904223;
        int coords = updateLCG >> 2;
        int x = (chunkX << 4) + (coords & 15);
        int z = (chunkZ << 4) + ((coords >> 8) & 15);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.canSeeSky(pos)) return updateLCG;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            level.addFreshEntity(bolt);
        }
        return updateLCG;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            UPDATE_LCG.remove(level.dimension().location().toString());
        }
    }

    public static void clearAllRuntimeState() {
        UPDATE_LCG.clear();
    }
}
