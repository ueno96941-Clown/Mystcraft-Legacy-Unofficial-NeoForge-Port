package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Converts semantic sky state into immutable renderer batches; no Minecraft client classes. */
public final class AgeSkyBatchPlanner {
    private AgeSkyBatchPlanner() {}
    public enum Mesh { NORMAL_STARS, TWINKLE_LAYER, RAINBOW, SUN_QUAD, MOON_QUAD, END_SKY_CUBE, SUNSET_FAN, HORIZON, VOID }
    public record Batch(Mesh mesh, int layer, float yawDegrees, float pitchDegrees,
                        float alpha, float size, boolean additive,int moonPhase,float red,float green,float blue,long symbolSeed,boolean randomizedPeriod,boolean randomizedAngle) {}

    public static List<Batch> plan(AgeSkyRenderBridge.RenderState s) {
        ArrayList<Batch> out=new ArrayList<>();
        if(s.endSkyBackground()) out.add(new Batch(Mesh.END_SKY_CUBE,0,0,0,1,100,false,0,1,1,1,0,false,false));
        for(var c:s.celestials()) { switch(c.kind()) {
            case SUN -> out.add(new Batch(Mesh.SUN_QUAD,0,c.angleDegrees(),c.altitudeTurns()*360F,c.alpha(),c.size(),true,0,1,1,1,0,false,false));
            case MOON -> out.add(new Batch(Mesh.MOON_QUAD,0,c.angleDegrees(),c.altitudeTurns()*360F,c.alpha(),c.size(),true,c.moonPhase(),1,1,1,0,false,false));
            case STARS_NORMAL -> { if(c.alpha()>0) out.add(new Batch(Mesh.NORMAL_STARS,0,c.angleDegrees(),c.altitudeTurns()*360F,c.alpha(),1,true,0,c.color().r(),c.color().g(),c.color().b(),0,false,false)); }
            case STARS_TWINKLE -> {
                float[] layers=c.twinkleLayers();
                if(layers!=null) for(int i=0;i<layers.length;i++) {
                    float a=c.alpha()*layers[i]; if(a>0) out.add(new Batch(Mesh.TWINKLE_LAYER,i,c.angleDegrees(),c.altitudeTurns()*360F,a,1,true,0,c.color().r(),c.color().g(),c.color().b(),c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()));
                }
            }
            case RAINBOW -> out.add(new Batch(Mesh.RAINBOW,0,c.angleDegrees(),0,c.alpha(),1,false,0,1,1,1,0,false,false));
            case STARS_END_SKY -> { /* background flag owns cube rendering */ }
        }
        if(c.sunset()!=null){var h=c.sunset();out.add(new Batch(Mesh.SUNSET_FAN,0,c.angleDegrees(),c.altitudeTurns()*360F,h.alpha(),1,false,0,h.red(),h.green(),h.blue(),0,false,false));}
        }
        if(s.drawHorizon()) out.add(new Batch(Mesh.HORIZON,0,0,0,1,1,false,0,1,1,1,0,false,false));
        if(s.drawVoid()) out.add(new Batch(Mesh.VOID,0,0,0,1,1,false,0,1,1,1,0,false,false));
        return List.copyOf(out);
    }
}