package com.xcompwiz.mystcraft.world.worldgen;
/** Exact vertical column/light-sample arithmetic from Legacy WeatherRendererMyst. */
public final class AgeWeatherColumnBoundsLegacyMath {
 private AgeWeatherColumnBoundsLegacyMath(){}
 /** Legacy k2: cameraFloorY-radius, raised to precipitation height when below it. */
 public static int lowerY(int cameraMinY,int precipitationY){return Math.max(cameraMinY,precipitationY);}
 /** Legacy l2: cameraFloorY+radius, independently raised to precipitation height. */
 public static int upperY(int cameraMaxY,int precipitationY){return Math.max(cameraMaxY,precipitationY);}
 /** Legacy i3: light is sampled at max(precipitation height, interpolated camera floor Y). */
 public static int lightSampleY(int precipitationY,double interpolatedCameraY){return Math.max(precipitationY,(int)Math.floor(interpolatedCameraY));}
}
