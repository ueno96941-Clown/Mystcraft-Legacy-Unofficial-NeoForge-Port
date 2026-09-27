package com.xcompwiz.mystcraft.validation;
import com.xcompwiz.mystcraft.world.worldgen.AgePrecipitationTickBridge;
public final class AgePrecipitationParticleSettingHarness{
 public static void main(String[]a){
  check(AgePrecipitationTickBridge.splashAttempts(1F,0)==100,"all");
  check(AgePrecipitationTickBridge.splashAttempts(1F,1)==50,"decreased");
  check(AgePrecipitationTickBridge.splashAttempts(1F,2)==0,"minimal");
  check(AgePrecipitationTickBridge.splashAttempts(.5F,0)==25,"quadratic");
  check(AgePrecipitationTickBridge.splashAttempts(.5F,1)==12,"half uses integer shift");
  System.out.println("AgePrecipitationParticleSettingHarness: PASS");
 }
 private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
