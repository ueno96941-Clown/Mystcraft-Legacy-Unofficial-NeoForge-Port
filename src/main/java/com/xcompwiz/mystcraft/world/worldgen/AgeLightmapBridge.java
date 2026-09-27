package com.xcompwiz.mystcraft.world.worldgen;
/** Pure math for the NeoForge 16x16 lightmap hook. pixelX is block light. */
public final class AgeLightmapBridge{
 private AgeLightmapBridge(){}
 public static float normalBrightness(int light){
  int i=Math.max(0,Math.min(15,light));float f=1F-i/15F;return (1F-f)/(f*3F+1F);
 }
 public static float brightnessDelta(AgeLightingPlan plan,int pixelX){
  return plan.brightnessTableValue(pixelX)-normalBrightness(pixelX);
 }
 public static void apply(float[] rgb,AgeLightingPlan plan,int pixelX){
  float d=brightnessDelta(plan,pixelX);
  rgb[0]=clamp(rgb[0]+d);rgb[1]=clamp(rgb[1]+d);rgb[2]=clamp(rgb[2]+d);
 }
 private static float clamp(float v){return Math.max(0,Math.min(1,v));}
}