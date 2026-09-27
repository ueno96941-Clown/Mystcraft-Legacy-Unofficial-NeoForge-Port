package com.xcompwiz.mystcraft.world.worldgen;
/** Pure Legacy 0.13.7.06 fast-cloud math, kept outside client code for deterministic testing. */
public final class AgeCloudRenderLegacyMath {
    public static final int MIN=-256, MAX=256, CELL=32;
    public static final double UV_SCALE=4.8828125E-4D; // 1/2048
    public static final double DRIFT_PER_TICK=0.029999999329447746D;
    private AgeCloudRenderLegacyMath() {}
    public static double wrap2048(double value){
        int section=(int)Math.floor(value/2048.0D);
        return value-section*2048.0D;
    }
    public static double driftedWrappedX(double cameraX,int ticks,float partial){
        return wrap2048(cameraX+((double)ticks+(double)partial)*DRIFT_PER_TICK);
    }
    public static double wrappedZ(double cameraZ){ return wrap2048(cameraZ); }
    public static float relativeY(float cloudHeight,double cameraY){ return cloudHeight-(float)cameraY+0.33F; }
    public static int quadCount(){ return ((MAX-MIN)/CELL)*((MAX-MIN)/CELL); }
}
