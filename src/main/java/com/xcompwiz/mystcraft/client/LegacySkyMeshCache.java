package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** CPU-side immutable legacy sky geometry cache. GPU buffers may be rebuilt from these exact meshes. */
public final class LegacySkyMeshCache {
    private LegacySkyMeshCache() {}
    private static final List<AgeSkyGeometry.Quad> NORMAL = AgeSkyGeometry.normalStars();
    private static final List<AgeSkyGeometry.Quad> RAINBOW = AgeSkyGeometry.rainbow();
    private static final List<AgeSkyGeometry.Quad> UPPER = AgeSkyGeometry.skyPlane(16,false);
    private static final List<AgeSkyGeometry.Quad> LOWER = AgeSkyGeometry.skyPlane(-16,true);
    private static final record TwinkleKey(long seed,boolean rp,boolean ra){}
    private static final ConcurrentHashMap<TwinkleKey,List<List<AgeSkyGeometry.Quad>>> TWINKLE = new ConcurrentHashMap<>();

    public static void prepare(long ageSeed, AgeSkyRenderBridge.RenderState state) {
        for (var c : state.celestials())
            if (c.kind()==AgeCelestialKind.STARS_TWINKLE) TWINKLE.computeIfAbsent(new TwinkleKey(c.symbolSeed(),c.randomizedPeriod(),c.randomizedAngle()),
                    k->AgeSkyGeometry.twinkleStars(k.seed(),k.rp(),k.ra()));
    }
    public static List<AgeSkyGeometry.Quad> normalStars(){return NORMAL;}
    public static List<AgeSkyGeometry.Quad> rainbow(){return RAINBOW;}
    public static List<AgeSkyGeometry.Quad> upperSky(){return UPPER;}
    public static List<AgeSkyGeometry.Quad> lowerSky(){return LOWER;}
    public static List<List<AgeSkyGeometry.Quad>> twinkle(long seed,boolean rp,boolean ra){
        return TWINKLE.computeIfAbsent(new TwinkleKey(seed,rp,ra),k->AgeSkyGeometry.twinkleStars(k.seed(),k.rp(),k.ra()));}
    public static void clearDynamic(){TWINKLE.clear();}
}
