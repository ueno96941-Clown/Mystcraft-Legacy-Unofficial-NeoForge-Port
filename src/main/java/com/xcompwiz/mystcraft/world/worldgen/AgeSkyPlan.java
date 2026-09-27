package com.xcompwiz.mystcraft.world.worldgen;
import java.util.List;
public record AgeSkyPlan(List<AgeCelestialPlan> celestials,boolean drawHorizon,boolean drawVoid,
 int horizonHeight,float cloudHeight,boolean endSkyBackground,AgeColorGradient endSkyGradient){
 public AgeSkyPlan{celestials=List.copyOf(celestials);}
}
