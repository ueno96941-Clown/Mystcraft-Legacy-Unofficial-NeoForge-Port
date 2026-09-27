package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherColumnBoundsLegacyMath;
public final class AgeWeatherColumnBoundsLegacyHarness {
 public static void main(String[] args){
  eq(90,AgeWeatherColumnBoundsLegacyMath.lowerY(90,50),"deep surface lower");
  eq(110,AgeWeatherColumnBoundsLegacyMath.upperY(110,50),"deep surface upper");
  eq(120,AgeWeatherColumnBoundsLegacyMath.lowerY(90,120),"surface above lower");
  eq(120,AgeWeatherColumnBoundsLegacyMath.upperY(110,120),"surface above upper/no draw");
  eq(100,AgeWeatherColumnBoundsLegacyMath.lightSampleY(50,100.9),"camera light sample");
  eq(120,AgeWeatherColumnBoundsLegacyMath.lightSampleY(120,100.9),"surface light sample");
  System.out.println("AgeWeatherColumnBoundsLegacyHarness: PASS");
 }
 private static void eq(int e,int a,String n){if(e!=a)throw new AssertionError(n+": expected "+e+" got "+a);}
}
