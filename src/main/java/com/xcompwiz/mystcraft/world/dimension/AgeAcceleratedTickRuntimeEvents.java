package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Replays 0.13.7.06 EffectExtraTicks for the EnvAccel Symbol. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeAcceleratedTickRuntimeEvents {
    private static final int EXTRA_ATTEMPTS_PER_RANDOM_TICKING_SECTION = 6;
    private static final Map<String, Integer> UPDATE_LCG = new ConcurrentHashMap<>();

    private AgeAcceleratedTickRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;

        var ageOptional = AgeManager.resolveByLevel(level.getServer(), level.dimension());
        if (ageOptional.isEmpty()) return;
        var prepared = AgeRuntimeDimensionLoader.getPrepared(ageOptional.get().ageUid()).orElse(null);
        if (prepared == null || !prepared.worldgenPlan().environmentPlan().acceleratedRandomTicks()) return;

        String key = level.dimension().location().toString();
        int lcg = UPDATE_LCG.computeIfAbsent(key, ignored -> level.getRandom().nextInt());
        HashSet<Long> visited = new HashSet<>();
        int simulationDistance = level.getServer().getPlayerList().getSimulationDistance();

        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dz = -simulationDistance; dz <= simulationDistance; dz++) {
                for (int dx = -simulationDistance; dx <= simulationDistance; dx++) {
                    int cx = center.x + dx;
                    int cz = center.z + dz;
                    long packed = ChunkPos.asLong(cx, cz);
                    if (!visited.add(packed)) continue;
                    if (!level.getChunkSource().chunkMap.getDistanceManager().inBlockTickingRange(packed)) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    lcg = tickChunk(level, chunk, lcg);
                }
            }
        }

        // Keep forced chunks participating even when no player is currently near them.
        for (long packed : level.getForcedChunks()) {
            if (!visited.add(packed)) continue;
            ChunkPos pos = new ChunkPos(packed);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) lcg = tickChunk(level, chunk, lcg);
        }
        UPDATE_LCG.put(key, lcg);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            String key = level.dimension().location().toString();
            UPDATE_LCG.remove(key);
        }
    }

    static int tickChunk(ServerLevel level, LevelChunk chunk, int updateLCG) {
        LevelChunkSection[] sections = chunk.getSections();
        int chunkBaseX = chunk.getPos().x << 4;
        int chunkBaseZ = chunk.getPos().z << 4;
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || !section.isRandomlyTickingBlocks()) continue;
            int sectionBaseY = level.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int i = 0; i < EXTRA_ATTEMPTS_PER_RANDOM_TICKING_SECTION; i++) {
                updateLCG = updateLCG * 3 + 1013904223;
                int bits = updateLCG >> 2;
                int x = bits & 15;
                int z = (bits >> 8) & 15;
                int y = (bits >> 16) & 15;
                BlockState state = section.getBlockState(x, y, z);
                if (!state.isRandomlyTicking()) continue;
                state.randomTick(level, new BlockPos(chunkBaseX + x, sectionBaseY + y, chunkBaseZ + z), level.getRandom());
            }
        }
        return updateLCG;
    }

    public static void clearAllRuntimeState() {
        UPDATE_LCG.clear();
    }
}
