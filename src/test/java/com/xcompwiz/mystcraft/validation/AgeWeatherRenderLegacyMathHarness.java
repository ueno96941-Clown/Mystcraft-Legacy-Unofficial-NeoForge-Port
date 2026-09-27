package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgeRainRenderBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeSnowRenderBridge;
/** Pure-Java checks for the ported WeatherRendererMyst column math. */
public final class AgeWeatherRenderLegacyMathHarness {
 public static void main(String[] args) {
  var rain=AgeRainRenderBridge.columns(100,0.5F,0,64,0,1F,10);
  var snow=AgeSnowRenderBridge.columns(100,0.5F,0,64,0,1F,10);
  require(!rain.isEmpty()&&!snow.isEmpty(),"weather columns missing");
  var r=rain.stream().filter(c->c.x()==10&&c.z()==0).findFirst().orElseThrow();
  require(close(r.xOffset(),0F)&&close(r.zOffset(),.5F),"Legacy radial rain quad vector changed");
  var s=snow.stream().filter(c->c.x()==10&&c.z()==10).findFirst().orElseThrow();
  require(s.alpha()>0F,"Legacy snow square-corner coverage lost");
  float expectedCorner=(float)(((1D-(10.5D*10.5D+10.5D*10.5D)/100D)*.3D+.5D));
  require(close(s.alpha(),expectedCorner),"Legacy snow corner alpha changed: "+s.alpha());
  var r2=AgeRainRenderBridge.columns(100,0.5F,0,64,0,1F,10).stream().filter(c->c.x()==3&&c.z()==4).findFirst().orElseThrow();
  require(Float.floatToIntBits(r2.scroll())==Float.floatToIntBits(AgeRainRenderBridge.columns(100,0.5F,0,64,0,1F,10).stream().filter(c->c.x()==3&&c.z()==4).findFirst().orElseThrow().scroll()),"rain RNG not deterministic");
  System.out.println("AgeWeatherRenderLegacyMathHarness: PASS");
 }
 private static boolean close(float a,float b){return Math.abs(a-b)<1e-5F;}
 private static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
}
