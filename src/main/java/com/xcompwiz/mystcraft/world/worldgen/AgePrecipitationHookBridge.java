package com.xcompwiz.mystcraft.world.worldgen;
/** Pure decisions for NeoForge precipitation hooks. */
public final class AgePrecipitationHookBridge{
 private AgePrecipitationHookBridge(){}
 public static boolean suppressVanilla(AgeWeatherPlan p){
  return p.mode()==AgeWeatherMode.OFF||p.mode()==AgeWeatherMode.CLOUDY;
 }
 public static boolean needsCustomSnowRenderer(AgeWeatherPlan p){return p.mode()==AgeWeatherMode.SNOW;}
 public static boolean needsCustomRainRenderer(AgeWeatherPlan p){return p.mode()==AgeWeatherMode.RAIN||p.mode()==AgeWeatherMode.STORM;}
}