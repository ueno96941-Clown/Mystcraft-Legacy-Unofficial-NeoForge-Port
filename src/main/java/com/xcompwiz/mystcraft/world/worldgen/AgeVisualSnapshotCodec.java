package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Locale;

/**
 * Small registry-independent codec for the client-visible subset of an Age plan.
 * This is intentionally versioned and primitive-only so dedicated clients never depend on the
 * server process-local PREPARED map.
 */
public final class AgeVisualSnapshotCodec {
    public static final int VERSION = 7;
    private AgeVisualSnapshotCodec() {}

    public static String encode(AgeWorldgenPlan p) {
        StringBuilder s = new StringBuilder(256);
        s.append("v=").append(VERSION);
        s.append(";light=").append(p.lightingPlan().mode().name());
        s.append(";horizon=").append(p.skyPlan().drawHorizon() ? 1 : 0);
        s.append(";void=").append(p.skyPlan().drawVoid() ? 1 : 0);
        s.append(";hh=").append(p.skyPlan().horizonHeight());
        s.append(";cloudh=").append(f(p.skyPlan().cloudHeight()));
        s.append(";endsky=").append(p.skyPlan().endSkyBackground() ? 1 : 0);
        s.append(";weather=").append(p.weatherPlan().mode().name());
        s.append(";cel=");
        boolean first = true;
        for (AgeCelestialPlan c : p.skyPlan().celestials()) {
            if (!first) s.append(',');
            first = false;
            s.append(c.kind().name()).append(':').append(c.periodTicks()).append(':')
                    .append(f(c.angleDegrees())).append(':').append(f(c.phaseOffset())).append(':')
                    .append(c.providesLight()?1:0).append(':').append(encodeGradientCompact(c.sunsetGradient())).append(':').append(encodeGradientCompact(c.starGradient())).append(':')
                    .append(c.symbolSeed()).append(':').append(c.randomizedPeriod()?1:0).append(':').append(c.randomizedAngle()?1:0);
        }
        appendColor(s, "sky", p.colorPlan().sky());
        appendColor(s, "fog", p.colorPlan().fog());
        appendColor(s, "cloud", p.colorPlan().cloud());
        appendColor(s, "water", p.colorPlan().water());
        appendColor(s, "grass", p.colorPlan().grass());
        appendColor(s, "foliage", p.colorPlan().foliage());
        appendGradient(s, "horizoncolor", p.colorPlan().horizonGradient());
        appendGradient(s, "endskycolor", p.skyPlan().endSkyGradient());
        return s.toString();
    }

    private static void appendColor(StringBuilder s, String key, java.util.List<AgeColorChannelPlan> providers) {
        s.append(';').append(key).append('=');
        boolean first = true;
        for (AgeColorChannelPlan c : providers) {
            if (!first) s.append('!');
            first = false;
            s.append(c.mode().name()).append(':').append(c.nightInverted() ? 1 : 0).append(':');
            if (c.staticColor() != null) appendRgb(s, c.staticColor());
            s.append(':');
            appendGradientBody(s, c.gradient());
        }
    }

    private static void appendGradient(StringBuilder s, String key, AgeColorGradient g) {
        s.append(';').append(key).append('=');
        appendGradientBody(s, g);
    }

    private static void appendGradientBody(StringBuilder s, AgeColorGradient g) {
        if (g == null) return;
        boolean first = true;
        for (AgeGradientPoint p : g.points()) {
            if (!first) s.append('|');
            first = false;
            appendRgb(s, p.color());
            s.append('@').append(f(p.interval()));
        }
    }

    private static void appendRgb(StringBuilder s, AgeColor c) {
        s.append(f(c.r())).append('/').append(f(c.g())).append('/').append(f(c.b()));
    }
    private static String f(float x) { return String.format(Locale.ROOT, "%.6g", x); }

    public static AgeVisualSnapshot decode(String encoded) {
        if (encoded == null) throw new IllegalArgumentException("Null Age visual snapshot");
        if (encoded.length() > 65536) throw new IllegalArgumentException("Age visual snapshot too large");
        java.util.HashMap<String,String> fields = new java.util.HashMap<>();
        for (String part : encoded.split(";", -1)) {
            int at = part.indexOf('=');
            if (at > 0) fields.put(part.substring(0, at), part.substring(at + 1));
        }
        int version = Integer.parseInt(required(fields, "v"));
        if (version != VERSION) throw new IllegalArgumentException("Unsupported Age visual snapshot v" + version);
        AgeLightingMode light = AgeLightingMode.valueOf(required(fields, "light"));
        boolean horizon = "1".equals(required(fields, "horizon"));
        boolean drawVoid = "1".equals(required(fields, "void"));
        int hh = Integer.parseInt(required(fields, "hh"));
        float cloudHeight = finite(required(fields, "cloudh"));
        boolean endSky = "1".equals(required(fields, "endsky"));
        AgeWeatherMode weather = AgeWeatherMode.valueOf(required(fields, "weather"));

        java.util.ArrayList<AgeCelestialPlan> cel = new java.util.ArrayList<>();
        String celestialText = fields.getOrDefault("cel", "");
        if (!celestialText.isEmpty()) for (String value : celestialText.split(",")) {
            String[] v = value.split(":", -1);
            if (v.length != 10) throw new IllegalArgumentException("Bad celestial");
            cel.add(new AgeCelestialPlan(AgeCelestialKind.valueOf(v[0]), Long.parseLong(v[1]),
                    finite(v[2]),finite(v[3]),"1".equals(v[4]),decodeGradientCompact(v[5]),decodeGradientCompact(v[6]),
                    Long.parseLong(v[7]),"1".equals(v[8]),"1".equals(v[9])));
        }

        java.util.HashMap<String,java.util.List<AgeColorChannelPlan>> colors = new java.util.HashMap<>();
        for (String key : new String[]{"sky","fog","cloud","water","grass","foliage"})
            colors.put(key, decodeColors(fields.getOrDefault(key, "")));
        return new AgeVisualSnapshot(version, light, horizon, drawVoid, hh, cloudHeight, endSky, weather, cel,
                colors,decodeGradient(fields.getOrDefault("horizoncolor","")),
                decodeGradient(fields.getOrDefault("endskycolor","")));
    }

    private static java.util.List<AgeColorChannelPlan> decodeColors(String text) {
        java.util.ArrayList<AgeColorChannelPlan> out = new java.util.ArrayList<>();
        if (text.isEmpty()) return out;
        for (String entry : text.split("!", -1)) {
            String[] p = entry.split(":", -1);
            if (p.length != 4) throw new IllegalArgumentException("Bad color channel");
            out.add(new AgeColorChannelPlan(AgeColorProviderMode.valueOf(p[0]), decodeGradient(p[3]),
                    p[2].isEmpty() ? null : decodeRgb(p[2]), "1".equals(p[1])));
        }
        return out;
    }

    private static AgeColorGradient decodeGradient(String text) {
        java.util.ArrayList<AgeGradientPoint> points = new java.util.ArrayList<>();
        if (!text.isEmpty()) for (String p : text.split("\\|")) {
            int at = p.lastIndexOf('@');
            if (at <= 0) throw new IllegalArgumentException("Bad gradient point");
            points.add(new AgeGradientPoint(decodeRgb(p.substring(0, at)), finite(p.substring(at + 1))));
        }
        return new AgeColorGradient(points);
    }

    private static AgeColor decodeRgb(String text) {
        String[] v = text.split("/");
        if (v.length != 3) throw new IllegalArgumentException("Bad RGB");
        return new AgeColor(finite(v[0]), finite(v[1]), finite(v[2]));
    }

    private static String encodeGradientCompact(AgeColorGradient g) {
        if(g==null||g.isEmpty()) return "";
        StringBuilder b=new StringBuilder(); boolean first=true;
        for(AgeGradientPoint p:g.points()){if(!first)b.append('~');first=false;
            b.append(f(p.color().r())).append('/').append(f(p.color().g())).append('/').append(f(p.color().b()))
             .append('@').append(f(p.interval()));}
        return b.toString();
    }
    private static AgeColorGradient decodeGradientCompact(String t) {
        if(t.isEmpty()) return null;
        java.util.ArrayList<AgeGradientPoint> p=new java.util.ArrayList<>();
        for(String x:t.split("~")){
            int a=x.lastIndexOf('@');
            if(a<=0 || a==x.length()-1) throw new IllegalArgumentException("Bad compact gradient point");
            p.add(new AgeGradientPoint(decodeRgb(x.substring(0,a)),finite(x.substring(a+1))));
        }
        return new AgeColorGradient(p);
    }

    private static float finite(String text) {
        float value = Float.parseFloat(text);
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite visual float");
        return value;
    }

    private static String required(java.util.Map<String,String> fields, String key) {
        String value = fields.get(key);
        if (value == null) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
}
