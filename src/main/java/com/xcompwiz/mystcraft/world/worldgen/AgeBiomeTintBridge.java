package com.xcompwiz.mystcraft.world.worldgen;
/** Pure Age-local static biome tint override. */
public final class AgeBiomeTintBridge{
 private AgeBiomeTintBridge(){}
 public enum Channel{GRASS,FOLIAGE,WATER}
 public static int resolve(AgeVisualSnapshot snap,Channel channel,int vanillaRgb){
  if(snap==null)return vanillaRgb;
  String key=switch(channel){case GRASS->"grass";case FOLIAGE->"foliage";case WATER->"water";};
  return AgeClientVisualBridge.color(snap.colors().get(key), vanillaRgb, 0F) & 0x00FFFFFF;
 }
}