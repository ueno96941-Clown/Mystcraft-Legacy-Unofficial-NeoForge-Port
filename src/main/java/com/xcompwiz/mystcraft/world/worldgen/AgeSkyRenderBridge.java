package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure renderer contract for the legacy Mystcraft sky.
 *
 * <p>It deliberately contains no OpenGL/NeoForge calls: modern render hooks consume the returned
 * immutable draw commands. Geometry/timing constants are taken from 0.13.7.06.</p>
 */
public final class AgeSkyRenderBridge {
    private AgeSkyRenderBridge() {}

    public record RenderState(
            boolean drawHorizon, boolean drawVoid, int horizonHeight,
            boolean endSkyBackground, AgeColor endSkyColor, List<CelestialDraw> celestials) {
        public RenderState { celestials = List.copyOf(celestials); }
    }

    public record CelestialDraw(
            AgeCelestialKind kind, float altitudeTurns, float angleDegrees,
            float alpha,float size,int starCount,float[] twinkleLayers,int moonPhase,AgeSunsetBridge.Sunset sunset,AgeColor color,long symbolSeed,boolean randomizedPeriod,boolean randomizedAngle) {}

    public static RenderState resolve(AgeSkyPlan plan, long worldTime, float partialTick,
                                      float rainStrength, float vanillaStarBrightness, long ageSeed) {
        ArrayList<CelestialDraw> out = new ArrayList<>();
        float dry = clamp(1.0F - rainStrength);
        for (AgeCelestialPlan c : plan.celestials()) {
            float altitude = celestialPeriod(c.periodTicks(), c.phaseOffset(), worldTime, partialTick);
            switch (c.kind()) {
                case SUN -> out.add(new CelestialDraw(c.kind(), altitude, c.angleDegrees(),
                        dry,30F,0,new float[0],0,sunset(c,altitude,worldTime,1F),new AgeColor(1,1,1),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                case MOON -> out.add(new CelestialDraw(c.kind(), altitude, c.angleDegrees(),
                        dry,20F,0,new float[0],moonPhase(c.periodTicks(),worldTime),sunset(c,altitude,worldTime,.3F),new AgeColor(1,1,1),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                case STARS_NORMAL -> out.add(new CelestialDraw(c.kind(), altitude, c.angleDegrees(),
                        clamp(vanillaStarBrightness*dry),0F,1500,new float[0],0,null,starColor(c,worldTime),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                case STARS_TWINKLE -> out.add(new CelestialDraw(c.kind(), altitude, c.angleDegrees(),
                        clamp(vanillaStarBrightness * dry), 0.0F, 1000,
                        twinkleBrightnesses(worldTime,partialTick,c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()),0,null,starColor(c,worldTime),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                case STARS_END_SKY -> out.add(new CelestialDraw(c.kind(), 0, 0,
                        dry,0F,0,new float[0],0,null,new AgeColor(1,1,1),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                case RAINBOW -> out.add(new CelestialDraw(c.kind(), 0, c.angleDegrees(),
                        1F,0F,0,new float[0],0,null,new AgeColor(1,1,1),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
            }
        }
        AgeColor ec=plan.endSkyGradient()==null||plan.endSkyGradient().isEmpty()?new AgeColor(.156F,.156F,.156F):plan.endSkyGradient().sample(worldTime/12000F);
        return new RenderState(plan.drawHorizon(),plan.drawVoid(),plan.horizonHeight(),plan.endSkyBackground(),ec,out);
    }

    /** Exact legacy eased celestial-period function. */
    public static float celestialPeriod(long period, float offset, long time, float partialTime) {
        if (period == 0L) return offset;
        long i = time % period;
        float f = (i + partialTime) / (float) period + offset;
        while (f < 0.0F) ++f;
        while (f > 1.0F) --f;
        float f1 = f;
        f = 1.0F - (float)((Math.cos(f * Math.PI) + 1.0D) / 2.0D);
        return f1 + (f - f1) / 3.0F;
    }

    /**
     * Legacy Twinkle used ten 100-star display lists, each with a deterministic seed-derived
     * time offset. We expose the ten layer brightnesses; modern code may use vertex buffers.
     */
    public static float[] twinkleBrightnesses(long time,float partial,long symbolSeed,boolean randomizedPeriod,boolean randomizedAngle) {
        Random rand=new Random(symbolSeed);
        if(randomizedPeriod)rand.nextDouble();
        if(randomizedAngle)rand.nextDouble();
        float[] out = new float[10];
        for (int i = 0; i < out.length; i++) {
            long offset = rand.nextLong();
            long t = (time + offset) % 100L;
            float f1 = (t + partial) / 100.0F;
            float f2 = 1.0F - ((float)Math.cos(f1 * Math.PI * 2.0F) * 2.0F + 0.25F);
            out[i] = clamp(f2);
        }
        return out;
    }

    private static float clamp(float v) { return Math.max(0F, Math.min(1F, v)); }
    public static int moonPhase(long periodTicks, long worldTime) {
        if (periodTicks == 0L) return 0;
        return (int)(worldTime / periodTicks) % 8;
    }

    private static AgeSunsetBridge.Sunset sunset(AgeCelestialPlan c,float altitude,long time,float scale){
        AgeSunsetBridge.Sunset base=AgeSunsetBridge.vanilla(altitude,scale);
        if(base==null)return null;
        if(c.sunsetGradient()==null||c.sunsetGradient().isEmpty())return base;
        AgeColor x=c.sunsetGradient().sample(time/12000F);
        return new AgeSunsetBridge.Sunset(x.r(),x.g(),x.b(),base.alpha());
    }

    private static AgeColor starColor(AgeCelestialPlan c,long time){
        return c.starGradient()==null||c.starGradient().isEmpty()?new AgeColor(1,1,1):c.starGradient().sample(time/12000F);
    }

}
