package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Exact non-anaglyph 0.13.7.06 cloud provider-list folding. */
public final class AgeCloudColorBridge {
    private AgeCloudColorBridge() {}

    public static AgeColor resolve(AgeVisualSnapshot snapshot, long worldTime, float celestialAngle,
                                   float rainStrength, float thunderStrength) {
        List<AgeColorChannelPlan> providers = snapshot.colors().get("cloud");
        AgeColor base = AgeColorProviderFold.fold(providers, channel -> {
            if (channel.mode()==AgeColorProviderMode.UNSET) return null;
            if (channel.mode()==AgeColorProviderMode.NATURAL) return new AgeColor(1F,1F,1F);
            if (channel.mode()==AgeColorProviderMode.CUSTOM_STATIC && channel.staticColor()!=null) return channel.staticColor();
            if (channel.mode()==AgeColorProviderMode.CUSTOM_DYNAMIC && channel.gradient()!=null && !channel.gradient().isEmpty())
                return channel.gradient().sample(worldTime/12000.0F);
            return null;
        });
        if (base==null) base=new AgeColor(1F,1F,1F);
        float r=base.r(),g=base.g(),b=base.b();
        float rain=clamp(rainStrength);
        if(rain>0F){float gray=(r*.3F+g*.59F+b*.11F)*.6F;float keep=1F-rain*.95F;r=r*keep+gray*(1F-keep);g=g*keep+gray*(1F-keep);b=b*keep+gray*(1F-keep);}
        float daylight=clamp((float)Math.cos(celestialAngle*Math.PI*2F)*2F+.5F);
        r*=daylight*.90F+.10F;g*=daylight*.90F+.10F;b*=daylight*.85F+.15F;
        float thunder=clamp(thunderStrength);
        if(thunder>0F){float gray=(r*.3F+g*.59F+b*.11F)*.2F;float keep=1F-thunder*.95F;r=r*keep+gray*(1F-keep);g=g*keep+gray*(1F-keep);b=b*keep+gray*(1F-keep);}
        return new AgeColor(r,g,b);
    }
    private static float clamp(float v){return Math.max(0F,Math.min(1F,v));}
}
