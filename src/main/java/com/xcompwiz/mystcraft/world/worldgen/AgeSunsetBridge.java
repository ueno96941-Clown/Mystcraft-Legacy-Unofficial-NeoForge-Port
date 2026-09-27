package com.xcompwiz.mystcraft.world.worldgen;

/** Exact non-anaglyph legacy SunsetRenderer intensity envelope. */
public final class AgeSunsetBridge {
 private AgeSunsetBridge(){}
 public record Sunset(float red,float green,float blue,float alpha){}
 public static Sunset vanilla(float celestialAngle,float alphaScale){
  float f3=(float)Math.cos(celestialAngle*Math.PI*2.0);
  if(f3<-.4F||f3>.4F)return null;
  float f5=(f3/.4F)*.5F+.5F;
  float f6=1F-(1F-(float)Math.sin(f5*Math.PI))*.99F; f6*=f6;
  return new Sunset(f5*.3F+.7F,f5*f5*.7F+.2F,.2F,f6*alphaScale);
 }
}
