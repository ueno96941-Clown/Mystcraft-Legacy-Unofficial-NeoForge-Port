package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.*;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Executes the Legacy 1.12 rain-splash/sound cadence for Mystcraft-forced precipitation. */
public final class AgePrecipitationTicker {
    private static int rainSoundCounter;

    private AgePrecipitationTicker() {}

    public static void tick(ClientLevel level, int ticks, Camera camera, AgeWeatherPlan weather) {
        float strength = weather.rainingStrength();
        if (strength <= 0 || !AgePrecipitationTickBridge.hasRainSplashes(weather.mode())) return;

        // Minecraft 1.12 EntityRenderer halved splash strength when fancy graphics was off
        // before applying the quadratic attempt count.
        if (!Minecraft.useFancyGraphics()) strength /= 2.0F;

        var p = camera.getPosition();
        int cx = (int) Math.floor(p.x);
        int cy = (int) Math.floor(p.y);
        int cz = (int) Math.floor(p.z);
        Random random = AgePrecipitationTickBridge.randomForTick(ticks);
        int valid = 0;
        double soundX = 0;
        double soundY = 0;
        double soundZ = 0;

        ParticleStatus particleStatus = Minecraft.getInstance().options.particles().get();
        int particleSetting = particleStatus == ParticleStatus.MINIMAL
                ? 2
                : particleStatus == ParticleStatus.DECREASED ? 1 : 0;
        int attempts = AgePrecipitationTickBridge.splashAttempts(strength, particleSetting);

        for (int i = 0; i < attempts; i++) {
            int x = cx + AgePrecipitationTickBridge.offset(random);
            int z = cz + AgePrecipitationTickBridge.offset(random);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y > cy + AgePrecipitationTickBridge.LEGACY_RADIUS
                    || y < cy - AgePrecipitationTickBridge.LEGACY_RADIUS) continue;

            // EntityRenderer#addRainParticles required canRain and positional temperature >=0.15.
            // Forced Rain/Storm overrides the biome flags, but the old altitude temperature
            // adjustment can still turn high columns into snow and therefore remove splashes.
            if (AgeLegacyPrecipitationClimateResolver.precipitation(level, x, y, z, weather)
                    != AgePrecipitationBridge.Type.RAIN) continue;

            BlockPos below = new BlockPos(x, y - 1, z);
            var state = level.getBlockState(below);
            if (state.isAir()) continue;

            // 1.12 sampled doubles here. Using floats changes both particle coordinates and
            // every downstream RNG decision, including the reservoir-selected rain sound.
            double ox = random.nextDouble();
            double oz = random.nextDouble();

            // IBlockState#getBoundingBox was relative to the block. VoxelShape is the modern
            // equivalent. Empty modern shapes (notably fluids) use the old full-cell fallback.
            var shape = state.getShape(level, below);
            double minY = 0.0D;
            double maxY = 1.0D;
            if (!shape.isEmpty()) {
                var bounds = shape.bounds();
                minY = bounds.minY;
                maxY = bounds.maxY;
            }

            double px = x + ox;
            double pz = z + oz;
            if (state.getFluidState().is(FluidTags.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
                double py = AgePrecipitationSurfaceLegacyMath.hotSurfaceSmokeY(y, minY);
                level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
                continue;
            }

            double particleY = AgePrecipitationSurfaceLegacyMath.rainParticleY(below.getY(), maxY);
            valid++;
            if (random.nextInt(valid) == 0) {
                soundX = px;
                soundY = AgePrecipitationSurfaceLegacyMath.rainSoundY(below.getY(), maxY);
                soundZ = pz;
            }
            level.addParticle(ParticleTypes.RAIN, px, particleY, pz, 0, 0, 0);
        }

        if (valid > 0 && AgePrecipitationTickBridge.shouldPlayRainSound(random, rainSoundCounter++)) {
            rainSoundCounter = 0;
            int cameraSurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
            if (soundY > p.y + 1.0D && cameraSurface > cy) {
                level.playLocalSound(soundX, soundY, soundZ,
                        SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, .1F, .5F, false);
            } else {
                level.playLocalSound(soundX, soundY, soundZ,
                        SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, .2F, 1F, false);
            }
        }
    }
}
