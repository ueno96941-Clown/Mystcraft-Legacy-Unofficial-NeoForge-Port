package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRenderDistanceContract;
public final class AgeWeatherRenderDistanceHarness {
 public static void main(String[] args){
  require(AgeWeatherRenderDistanceContract.radius(false)==5,"fast radius");
  require(AgeWeatherRenderDistanceContract.radius(true)==10,"fancy radius");
  require((2*AgeWeatherRenderDistanceContract.radius(false)+1)==11,"fast width");
  require((2*AgeWeatherRenderDistanceContract.radius(true)+1)==21,"fancy width");
  System.out.println("AgeWeatherRenderDistanceHarness: PASS");
 }
 private static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
}
