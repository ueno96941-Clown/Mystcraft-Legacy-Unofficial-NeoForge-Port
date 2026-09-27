package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgeCloudFancyLegacyMath;
public final class AgeCloudFancyLegacyMathHarness {
 public static void main(String[] a){
  eq(AgeCloudFancyLegacyMath.cellCount(),64,"8x8 cells");
  near(AgeCloudFancyLegacyMath.driftX(120,0,0),10,"camera scale");
  near(AgeCloudFancyLegacyMath.driftZ(120),10.330000013113022,"z bias");
  near(AgeCloudFancyLegacyMath.relativeY(128,100),28.33,"height");
  near(AgeCloudFancyLegacyMath.shadeBottom(),.7,"bottom shade");
  near(AgeCloudFancyLegacyMath.shadeX(),.9,"x shade");
  near(AgeCloudFancyLegacyMath.shadeZ(),.8,"z shade");
  near(AgeCloudFancyLegacyMath.uvBase(10.75),10*.00390625,"uv floor base");
  near(AgeCloudFancyLegacyMath.frac(10.75),.75,"fraction");
  System.out.println("AgeCloudFancyLegacyMathHarness: PASS");
 }
 static void eq(int a,int b,String n){if(a!=b)throw new AssertionError(n+": "+a);}
 static void near(double a,double b,String n){if(Math.abs(a-b)>1e-6)throw new AssertionError(n+": "+a+" != "+b);}
}
