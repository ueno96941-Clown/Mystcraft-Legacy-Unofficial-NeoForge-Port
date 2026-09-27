package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Maps legacy weather Symbols to the exact 0.13.7.06 controller constants. */
public final class AgeWeatherResolver {
    private AgeWeatherResolver() {}

    public static AgeWeatherPlan resolve(List<String> effectiveSymbols) {
        AgeWeatherMode selected = AgeWeatherMode.UNKNOWN;
        for (String symbol : effectiveSymbols) {
            switch (symbol) {
                case "mystcraft:WeatherOn" -> selected = AgeWeatherMode.ALWAYS;
                case "mystcraft:WeatherCloudy" -> selected = AgeWeatherMode.CLOUDY;
                case "mystcraft:WeatherOff" -> selected = AgeWeatherMode.OFF;
                case "mystcraft:WeatherRain" -> selected = AgeWeatherMode.RAIN;
                case "mystcraft:WeatherSnow" -> selected = AgeWeatherMode.SNOW;
                case "mystcraft:WeatherStorm" -> selected = AgeWeatherMode.STORM;
                case "mystcraft:WeatherFast" -> selected = AgeWeatherMode.FAST;
                case "mystcraft:WeatherNorm" -> selected = AgeWeatherMode.NORMAL;
                case "mystcraft:WeatherSlow" -> selected = AgeWeatherMode.SLOW;
                default -> { }
            }
        }
        // Legacy registerInterface(IWeatherController) replaced any earlier controller.
        return resolveMode(selected);
    }

    public static AgeWeatherPlan resolveMode(AgeWeatherMode mode) {
        return switch(mode) {
            case ALWAYS -> toggle(AgeWeatherMode.ALWAYS,1F,0F,null,null,null,null);
            case CLOUDY -> toggle(AgeWeatherMode.CLOUDY,1F,0F,false,false,null,null);
            case OFF -> toggle(AgeWeatherMode.OFF,0F,0F,null,null,null,null);
            case RAIN -> toggle(AgeWeatherMode.RAIN,1F,0F,true,false,.20F,null);
            case SNOW -> toggle(AgeWeatherMode.SNOW,1F,0F,true,true,null,.10F);
            case STORM -> toggle(AgeWeatherMode.STORM,1F,1F,true,false,.20F,null);
            case FAST -> cycle(AgeWeatherMode.FAST,6000,6000,84000,6000,6000,1800,84000,6000);
            case NORMAL -> cycle(AgeWeatherMode.NORMAL,12000,12000,168000,12000,12000,3600,168000,12000);
            case SLOW -> cycle(AgeWeatherMode.SLOW,24000,24000,336000,24000,24000,7200,336000,24000);
            default -> new AgeWeatherPlan(AgeWeatherMode.UNKNOWN,false,0F,0F,null,null,null,null,0,0,0,0,0,0,0,0);
        };
    }

    private static AgeWeatherPlan toggle(
            AgeWeatherMode mode, float rain, float thunder,
            Boolean rainEnabled, Boolean snowEnabled,
            Float minimumTemperature, Float maximumTemperature) {
        return new AgeWeatherPlan(mode,true,rain,thunder,rainEnabled,snowEnabled,
                minimumTemperature,maximumTemperature,0,0,0,0,0,0,0,0);
    }

    private static AgeWeatherPlan cycle(
            AgeWeatherMode mode,
            int rd,int rdb,int rc,int rcb,
            int td,int tdb,int tc,int tcb) {
        return new AgeWeatherPlan(mode,false,0F,0F,null,null,null,null,
                rd,rdb,rc,rcb,td,tdb,tc,tcb);
    }
}
