package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

public final class AgeClientVisualBridge {
    private AgeClientVisualBridge() {}
    public static float[] brightnessTable(AgeLightingPlan plan){float[] t=new float[16];for(int i=0;i<t.length;i++)t[i]=plan.brightnessTableValue(i);return t;}
    public static int scalePackedBlockLight(AgeLightingPlan plan,int packedLight){
        // Minecraft 1.21 LightTexture.pack(block, sky): block occupies bits 4..19, sky bits 20..31.
        // The previous low-nibble extraction always read zero for normal packed light values.
        int block=(packedLight>>>4)&0xFFFF;
        int scaled=plan.scaleBlockLight(block);
        return (packedLight&~0x000FFFF0)|((scaled&0xFFFF)<<4);
    }
    public static int color(List<AgeColorChannelPlan> providers,int vanillaRgb,float celestialAngle){
        AgeColor natural=fromRgb(vanillaRgb);
        AgeColor c=AgeColorProviderFold.fold(providers,p->{
            if(p.mode()==AgeColorProviderMode.UNSET)return null;
            if(p.mode()==AgeColorProviderMode.NATURAL)return natural;
            if(p.mode()==AgeColorProviderMode.CUSTOM_STATIC)return p.staticColor();
            if(p.gradient()==null||p.gradient().isEmpty())return null;
            float sample=p.nightInverted()?1F-celestialAngle:celestialAngle;return p.gradient().sample(sample);
        });
        return c==null?vanillaRgb:rgb(c);
    }
    public static int rgb(AgeColor c){if(c==null)return 0xFFFFFF;int r=Math.round(clamp(c.r())*255F),g=Math.round(clamp(c.g())*255F),b=Math.round(clamp(c.b())*255F);return(r<<16)|(g<<8)|b;}
    private static AgeColor fromRgb(int rgb){return new AgeColor(((rgb>>16)&255)/255F,((rgb>>8)&255)/255F,(rgb&255)/255F);}
    private static float clamp(float v){return Math.max(0F,Math.min(1F,v));}
}
