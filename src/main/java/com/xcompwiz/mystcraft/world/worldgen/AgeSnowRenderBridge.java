package com.xcompwiz.mystcraft.world.worldgen;
import java.util.*;
/** Legacy-shaped forced-snow column planner for WeatherSnow. */
public final class AgeSnowRenderBridge{
 private AgeSnowRenderBridge(){}
 public record Column(int x,int z,int minY,int maxY,float alpha,float xOffset,float zOffset,float uOffset,float vOffset){}
 public static List<Column> columns(int ticks,float partial,double camX,double camY,double camZ,float strength,int radius){
  if(strength<=0||radius<=0)return List.of();
  int cx=(int)Math.floor(camX),cy=(int)Math.floor(camY),cz=(int)Math.floor(camZ);
  ArrayList<Column> out=new ArrayList<>((radius*2+1)*(radius*2+1));
  float legacyTime=0F; // Legacy f1 is initialized to zero in WeatherRendererMyst.
  for(int z=cz-radius;z<=cz+radius;z++)for(int x=cx-radius;x<=cx+radius;x++){
   int gx=x-cx,gz=z-cz;double len=Math.sqrt((double)gx*gx+(double)gz*gz);
   float xo=len==0?0.5F:(float)(-gz/len*.5D),zo=len==0?0F:(float)(gx/len*.5D);
   double dx=x+.5-camX,dz=z+.5-camZ;float dist=(float)(Math.sqrt(dx*dx+dz*dz)/radius);
   float alpha=((1F-dist*dist)*.3F+.5F)*strength;
   if(alpha<=0)continue;
   long legacySeed=(long)(x*x*3121+x*45238971 ^ z*z*418711+z*13761);
   Random random=new Random(legacySeed);
   float u=(float)(random.nextDouble()+legacyTime*.01F*random.nextGaussian());
   float v=(float)(-((ticks&511)+partial)/512F + random.nextDouble()+legacyTime*random.nextGaussian()*.001F);
   out.add(new Column(x,z,cy-radius,cy+radius,alpha,xo,zo,u,v));
  }
  return List.copyOf(out);
 }
}
