package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgeCloudRenderLegacyMath;
public final class AgeCloudRenderLegacyMathHarness {
 private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 private static void near(double a,double b,double eps,String msg){check(Math.abs(a-b)<=eps,msg+": "+a+" != "+b);}
 public static void main(String[] args){
  check(AgeCloudRenderLegacyMath.quadCount()==256,"Legacy fast clouds must be 16x16=256 quads");
  near(AgeCloudRenderLegacyMath.UV_SCALE,1.0/2048.0,1e-15,"UV scale");
  near(AgeCloudRenderLegacyMath.driftedWrappedX(0,100,0F),100D*0.029999999329447746D,1e-12,"X drift");
  near(AgeCloudRenderLegacyMath.wrap2048(2050),2,0,"positive wrap");
  near(AgeCloudRenderLegacyMath.wrap2048(-1),2047,0,"negative wrap follows floor");
  near(AgeCloudRenderLegacyMath.relativeY(128F,100D),28.33F,1e-4,"cloud Y bias");
  System.out.println("AgeCloudRenderLegacyMathHarness: PASS");
 }
}
