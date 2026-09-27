package com.xcompwiz.mystcraft.world.worldgen;
import java.util.Random;
/** Pure Legacy 1.12 rain-splash math used by the client precipitation ticker. */
public final class AgePrecipitationTickBridge{
 private AgePrecipitationTickBridge(){}
 public static final int LEGACY_RADIUS=10;
 public static final long LEGACY_SEED_MULTIPLIER=312987231L;
 /** Legacy EntityRenderer#addRainParticles emitted no snow splash particles. */
 public static boolean hasRainSplashes(AgeWeatherMode mode){return mode!=AgeWeatherMode.SNOW;}
 public static int splashAttempts(float strength){return splashAttempts(strength,0);}
 /** Legacy GameSettings.particleSetting contract: 0=all, 1=decreased (half), 2=minimal (zero). */
 public static int splashAttempts(float strength,int particleSetting){
  if(strength<=0)return 0;
  int count=(int)(100.0F*strength*strength);
  if(particleSetting==1)return count>>1;
  if(particleSetting>=2)return 0;
  return count;
 }
 public static Random randomForTick(int ticks){return new Random((long)ticks*LEGACY_SEED_MULTIPLIER);}
 public static int offset(Random random){return random.nextInt(LEGACY_RADIUS)-random.nextInt(LEGACY_RADIUS);}
 public static boolean shouldPlayRainSound(Random random,int rainSoundCounter){return random.nextInt(3)<rainSoundCounter;}
}
