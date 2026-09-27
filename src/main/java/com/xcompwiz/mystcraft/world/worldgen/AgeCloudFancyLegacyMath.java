package com.xcompwiz.mystcraft.world.worldgen;
/** Pure Legacy 0.13.7.06 fancy-cloud coordinate/UV contract. */
public final class AgeCloudFancyLegacyMath {
    public static final int MIN_CELL=-3, MAX_CELL=4, CELL=8, SIDE_STRIPS=8;
    public static final float WORLD_SCALE=12F, HEIGHT=4F, TOP_EPSILON=9.765625E-4F;
    public static final double UV_SCALE=0.00390625D; // 1/256 in scaled cloud coordinates
    private AgeCloudFancyLegacyMath() {}
    public static double driftX(double cameraX,int ticks,float partial){
        return (cameraX+((double)ticks+(double)partial)*AgeCloudRenderLegacyMath.DRIFT_PER_TICK)/12.0D;
    }
    public static double driftZ(double cameraZ){ return cameraZ/12.0D+0.33000001311302185D; }
    public static double wrap2048(double v){ return AgeCloudRenderLegacyMath.wrap2048(v); }
    public static float frac(double v){ return (float)(v-Math.floor(v)); }
    public static float uvBase(double v){ return (float)Math.floor(v)*0.00390625F; }
    public static float relativeY(float cloudHeight,double cameraY){ return cloudHeight-(float)cameraY+0.33F; }
    public static int cellCount(){ return (MAX_CELL-MIN_CELL+1)*(MAX_CELL-MIN_CELL+1); }
    public static float shadeX(){ return .9F; }
    public static float shadeZ(){ return .8F; }
    public static float shadeBottom(){ return .7F; }
}
