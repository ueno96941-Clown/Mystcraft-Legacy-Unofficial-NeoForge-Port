package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeColor;
import com.xcompwiz.mystcraft.world.worldgen.AgeSkyWeatherTintBridge;

/** Pure-Java regression vectors for Legacy WorldProviderMyst rain/thunder sky attenuation. */
public final class AgeSkyWeatherTintHarness {
    private static void near(float actual,float expected){ if(Math.abs(actual-expected)>0.00001F) throw new AssertionError(actual+" != "+expected); }
    public static void main(String[] args){
        AgeColor base=new AgeColor(1F,.5F,.25F);
        AgeColor clear=AgeSkyWeatherTintBridge.apply(base,0F,0F);
        near(clear.r(),1F); near(clear.g(),.5F); near(clear.b(),.25F);
        // Independent reproduction of the Legacy statement order: rain first, thunder second.
        float r=1F,g=.5F,b=.25F;
        float gray=(r*.3F+g*.59F+b*.11F)*.6F, keep=1F-.8F*.75F;
        r=r*keep+gray*(1F-keep); g=g*keep+gray*(1F-keep); b=b*keep+gray*(1F-keep);
        gray=(r*.3F+g*.59F+b*.11F)*.2F; keep=1F-.4F*.75F;
        r=r*keep+gray*(1F-keep); g=g*keep+gray*(1F-keep); b=b*keep+gray*(1F-keep);
        AgeColor storm=AgeSkyWeatherTintBridge.apply(base,.8F,.4F);
        near(storm.r(),r); near(storm.g(),g); near(storm.b(),b);
        System.out.println("AgeSkyWeatherTintHarness: PASS");
    }
}
