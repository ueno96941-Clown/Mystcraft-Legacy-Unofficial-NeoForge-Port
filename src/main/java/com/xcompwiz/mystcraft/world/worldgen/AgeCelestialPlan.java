package com.xcompwiz.mystcraft.world.worldgen;
public record AgeCelestialPlan(
 AgeCelestialKind kind,long periodTicks,float angleDegrees,float phaseOffset,boolean providesLight,
 AgeColorGradient sunsetGradient,AgeColorGradient starGradient,long symbolSeed,boolean randomizedPeriod,boolean randomizedAngle) {}
