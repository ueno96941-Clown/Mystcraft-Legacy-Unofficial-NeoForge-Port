package com.xcompwiz.mystcraft.world.worldgen;
import java.util.*;
/** Legacy-shaped forced-rain column planner for WeatherRain/WeatherStorm. */
public final class AgeRainRenderBridge{
 private AgeRainRenderBridge(){}
 /** xOffset/zOffset are the Legacy rainXCoords/rainYCoords half-width vector. */
 public record Column(int x,int z,int minY,int maxY,float alpha,float xOffset,float zOffset,float scroll){}
 public static List<Column> columns(int ticks,float partial,double camX,double camY,double camZ,float strength,int radius){
  if(strength<=0||radius<=0)return List.of();
  int cx=(int)Math.floor(camX),cy=(int)Math.floor(camY),cz=(int)Math.floor(camZ);
  ArrayList<Column> out=new ArrayList<>((radius*2+1)*(radius*2+1));
  for(int z=cz-radius;z<=cz+radius;z++)for(int x=cx-radius;x<=cx+radius;x++){
   int gx=x-cx,gz=z-cz;double len=Math.sqrt((double)gx*gx+(double)gz*gz);
   // Legacy's precomputed 32x32 direction field is undefined only at the camera column.
   float xo=len==0?0.5F:(float)(-gz/len*.5D),zo=len==0?0F:(float)(gx/len*.5D);
   double dx=x+.5-camX,dz=z+.5-camZ;float dist=(float)(Math.sqrt(dx*dx+dz*dz)/radius);
   float alpha=((1F-dist*dist)*.5F+.5F)*strength;
   if(alpha<=0)continue;
   long legacySeed=(long)(x*x*3121+x*45238971 ^ z*z*418711+z*13761);
   Random random=new Random(legacySeed);
   double scroll=-((double)((ticks+x*x*3121+x*45238971+z*z*418711+z*13761)&31)+partial)/32D*(3D+random.nextDouble());
   out.add(new Column(x,z,cy-radius,cy+radius,alpha,xo,zo,(float)scroll));
  }
  return List.copyOf(out);
 }
}
