package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure CPU compatibility geometry for Mystcraft 0.13.7.06 sky behavior. */
public final class AgeSkyGeometry {
    private AgeSkyGeometry() {}
    public record Vertex(float x,float y,float z,float r,float g,float b,float a) {}
    public record Quad(Vertex a,Vertex b,Vertex c,Vertex d) {}

    /** Legacy SymbolStarsNormal: seed 10842, 1500 attempts, radius 100, size .15-.25. */
    public static List<Quad> normalStars() { return stars(new Random(10842L), 1500); }

    /** Legacy SymbolStarsTwinkle: ten consecutive 100-attempt layers from the celestial RNG. */
    public static List<List<Quad>> twinkleStars(long seed,boolean randomizedPeriod,boolean randomizedAngle) {
        Random r=new Random(seed);
        if(randomizedPeriod) r.nextDouble();
        if(randomizedAngle) r.nextDouble();
        for(int i=0;i<10;i++) r.nextLong(); // legacy initialize() consumes offsets first
        ArrayList<List<Quad>> out=new ArrayList<>();
        for(int i=0;i<10;i++) out.add(stars(r,100));
        return List.copyOf(out);
    }

    /** Compatibility overload; assumes both constructor modifiers were explicit. */
    public static List<List<Quad>> twinkleStars(long seed){return twinkleStars(seed,false,false);}

    private static List<Quad> stars(Random random,int attempts) {
        ArrayList<Quad> out=new ArrayList<>();
        for(int i=0;i<attempts;i++) {
            double x=random.nextFloat()*2F-1F,y=random.nextFloat()*2F-1F,z=random.nextFloat()*2F-1F;
            double size=.15F+random.nextFloat()*.1F,d=x*x+y*y+z*z;
            if(d>=1D||d<=.01D) continue;
            d=1D/Math.sqrt(d);x*=d;y*=d;z*=d;
            double cx=x*100D,cy=y*100D,cz=z*100D;
            double az=Math.atan2(x,z), saz=Math.sin(az),caz=Math.cos(az);
            double pol=Math.atan2(Math.sqrt(x*x+z*z),y),sp=Math.sin(pol),cp=Math.cos(pol);
            double roll=random.nextDouble()*Math.PI*2D,sr=Math.sin(roll),cr=Math.cos(roll);
            Vertex[] v=new Vertex[4];
            for(int j=0;j<4;j++) {
                double qx=((j&2)-1)*size,qy=((j+1&2)-1)*size;
                double rx=qx*cr-qy*sr, ry=qy*cr+qx*sr;
                double px=rx*sp, py=-rx*cp;
                double ox=py*saz-ry*caz, oz=ry*saz+py*caz;
                v[j]=new Vertex((float)(cx+ox),(float)(cy+px),(float)(cz+oz),1,1,1,1);
            }
            out.add(new Quad(v[0],v[1],v[2],v[3]));
        }
        return List.copyOf(out);
    }

    /** Legacy RenderRainbow: 50 strips, quadratic arch, 0.2 alpha. */
    public static List<Quad> rainbow() {
        ArrayList<Quad> out=new ArrayList<>(); int strips=50; float width=.5F,total=width*strips,res=100F,step=.5F;
        for(int line=0;line<strips;line++) {
            float hue=(line/(float)strips)*240F; float[] rgb=hsv(hue,1F,200F/255F);
            for(float z=-res;z<=res;z+=step) {
                float y=quadratic(z/res)*res, y2=quadratic((z-step)/res)*res;
                float x0=line*width-width-total/2F,x1=line*width+width-total/2F;
                out.add(new Quad(v(x0,y2,z,rgb,.2F),v(x1,y2,z,rgb,.2F),
                        v(x1,y,z+step,rgb,.2F),v(x0,y,z+step,rgb,.2F)));
            }
        } return List.copyOf(out);
    }
    private static float quadratic(float x){ return 1F-x*x; }
    private static Vertex v(float x,float y,float z,float[] c,float a){return new Vertex(x,y,z,c[0],c[1],c[2],a);}
    private static float[] hsv(float h,float s,float v){
        float c=v*s,x=c*(1-Math.abs((h/60F)%2-1)),m=v-c; float r=0,g=0,b=0;
        if(h<60){r=c;g=x;}else if(h<120){r=x;g=c;}else if(h<180){g=c;b=x;}else if(h<240){g=x;b=c;}else if(h<300){r=x;b=c;}else{r=c;b=x;}
        return new float[]{r+m,g+m,b+m};
    }

    /**
     * CP236 legacy-result sky/fog shell. 0.13.7.06 did not blend ColorFog across the whole
     * hemisphere: it drew a ColorSky upper plane with fixed-function fog, so fog influence
     * became strong only close to the apparent horizon where the plane was far from the camera.
     *
     * Modern shaders do not reproduce that old distance composition, so we encode the same
     * visual result into geometry. The zenith and most of the sky remain pure ColorSky; only
     * the lowest ~16 degrees above the horizon transition smoothly into ColorFog. This also
     * preserves the useful CP235 RGB interpolation while moving its boundary to the correct
     * place instead of spreading Fog from horizon to zenith.
     */
    public static List<Quad> verticalGradientDome(AgeColor zenith, AgeColor horizon) {
        final int sectors = 64;
        final int bands = 32;
        final float radius = 512F;
        final float fogBlendEndSin = (float)Math.sin(Math.toRadians(16.0));
        ArrayList<Quad> out = new ArrayList<>();
        for (int band = 0; band < bands; band++) {
            double p0 = (Math.PI * 0.5) * band / bands;
            double p1 = (Math.PI * 0.5) * (band + 1) / bands;
            float y0 = (float)(Math.cos(p0) * radius);
            float y1 = (float)(Math.cos(p1) * radius);
            float r0 = (float)(Math.sin(p0) * radius);
            float r1 = (float)(Math.sin(p1) * radius);

            // y/radius is sin(elevation above the horizon): 0 at horizon, 1 at zenith.
            // Fog owns the horizon and fades out completely by about 16 degrees elevation.
            float skyWeight0 = smooth(clamp01((y0 / radius) / fogBlendEndSin));
            float skyWeight1 = smooth(clamp01((y1 / radius) / fogBlendEndSin));
            AgeColor c0 = mix(horizon, zenith, skyWeight0);
            AgeColor c1 = mix(horizon, zenith, skyWeight1);
            for (int sector = 0; sector < sectors; sector++) {
                double a0 = Math.PI * 2.0 * sector / sectors;
                double a1 = Math.PI * 2.0 * (sector + 1) / sectors;
                Vertex a = cv((float)(Math.cos(a0)*r0), y0, (float)(Math.sin(a0)*r0), c0);
                Vertex b = cv((float)(Math.cos(a1)*r0), y0, (float)(Math.sin(a1)*r0), c0);
                Vertex c = cv((float)(Math.cos(a1)*r1), y1, (float)(Math.sin(a1)*r1), c1);
                Vertex d = cv((float)(Math.cos(a0)*r1), y1, (float)(Math.sin(a0)*r1), c1);
                out.add(new Quad(a,b,c,d));
            }
        }
        // Below eye level the old upper plane was already fully fogged. Keep a pure horizon
        // colour skirt so looking slightly downward cannot expose the modern dome edge.
        for (int sector = 0; sector < sectors; sector++) {
            double a0 = Math.PI * 2.0 * sector / sectors;
            double a1 = Math.PI * 2.0 * (sector + 1) / sectors;
            Vertex a = cv((float)(Math.cos(a0)*radius), 0F, (float)(Math.sin(a0)*radius), horizon);
            Vertex b = cv((float)(Math.cos(a1)*radius), 0F, (float)(Math.sin(a1)*radius), horizon);
            Vertex c = cv((float)(Math.cos(a1)*radius), -128F, (float)(Math.sin(a1)*radius), horizon);
            Vertex d = cv((float)(Math.cos(a0)*radius), -128F, (float)(Math.sin(a0)*radius), horizon);
            out.add(new Quad(a,b,c,d));
        }
        return List.copyOf(out);
    }

    private static float smooth(float t) { return t*t*(3F-2F*t); }
    private static float clamp01(float v) { return Math.max(0F, Math.min(1F, v)); }
    private static AgeColor mix(AgeColor a, AgeColor b, float t) {
        return new AgeColor(a.r()+(b.r()-a.r())*t, a.g()+(b.g()-a.g())*t, a.b()+(b.b()-a.b())*t);
    }
    private static Vertex cv(float x,float y,float z,AgeColor c) { return new Vertex(x,y,z,c.r(),c.g(),c.b(),1F); }

    /** Legacy 13x13 sky plane (-384..384 inclusive, step 64) at supplied Y. */
    public static List<Quad> skyPlane(float y, boolean reverse) {
        return skyPlane(y, reverse, 384, 64);
    }

    /**
     * Modern cover mesh for the legacy upper sky. 1.12 could use the small -384..384
     * grid because fixed-function fog hid its outer edge. In 1.21 the same finite edge can
     * become a visible horizontal cutoff before fog has reached full opacity. Keep the same
     * flat-plane semantics, but extend it well past any ordinary terrain fog distance so the
     * active shader fog owns the sky-to-horizon transition just as it did in 1.12.
     */
    public static List<Quad> skyPlaneFogCover(float y, boolean reverse) {
        return skyPlane(y, reverse, 2048, 128);
    }

    private static List<Quad> skyPlane(float y, boolean reverse, int radius, int step) {
        ArrayList<Quad> out=new ArrayList<>();
        for(int x=-radius;x<radius;x+=step) for(int z=-radius;z<radius;z+=step) {
            if(!reverse) out.add(new Quad(p(x,y,z),p(x+step,y,z),p(x+step,y,z+step),p(x,y,z+step)));
            else out.add(new Quad(p(x+step,y,z),p(x,y,z),p(x,y,z+step),p(x+step,y,z+step)));
        }
        return List.copyOf(out);
    }
    private static Vertex p(float x,float y,float z){return new Vertex(x,y,z,1,1,1,1);}

    public static List<Quad> celestialQuad(float size) {
        return List.of(new Quad(p(-size,100,-size),p(size,100,-size),p(size,100,size),p(-size,100,size)));
    }
    /** Six inward-facing 200x200 cube faces, legacy End sky dimensions. */
    public static List<Quad> endSkyCube() {
        ArrayList<Quad> q=new ArrayList<>();
        q.add(new Quad(p(-100,-100,-100),p(-100,-100,100),p(100,-100,100),p(100,-100,-100)));
        q.add(new Quad(p(-100,100,-100),p(100,100,-100),p(100,100,100),p(-100,100,100)));
        q.add(new Quad(p(-100,-100,-100),p(100,-100,-100),p(100,100,-100),p(-100,100,-100)));
        q.add(new Quad(p(100,-100,100),p(-100,-100,100),p(-100,100,100),p(100,100,100)));
        q.add(new Quad(p(-100,-100,100),p(-100,-100,-100),p(-100,100,-100),p(-100,100,100)));
        q.add(new Quad(p(100,-100,-100),p(100,-100,100),p(100,100,100),p(100,100,-100)));
        return List.copyOf(q);
    }

    /** Old SkyRendererMyst black enclosure below the horizon. */
    public static List<Quad> voidEnclosure(float topY) {
        ArrayList<Quad> q=new ArrayList<>();
        q.add(new Quad(p(-1,topY,1),p(1,topY,1),p(1,-1,1),p(-1,-1,1)));
        q.add(new Quad(p(-1,-1,-1),p(1,-1,-1),p(1,topY,-1),p(-1,topY,-1)));
        q.add(new Quad(p(1,-1,-1),p(1,-1,1),p(1,topY,1),p(1,topY,-1)));
        q.add(new Quad(p(-1,topY,-1),p(-1,topY,1),p(-1,-1,1),p(-1,-1,-1)));
        q.add(new Quad(p(-1,-1,-1),p(-1,-1,1),p(1,-1,1),p(1,-1,-1)));
        return List.copyOf(q);
    }

    public record FanVertex(float x,float y,float z,float alpha) {}
    /** Legacy SunsetRenderer triangle fan before matrix rotations. Center + 17 rim vertices. */
    public static List<FanVertex> sunsetFan(float alpha) {
        ArrayList<FanVertex> out=new ArrayList<>();
        out.add(new FanVertex(0,100,0,alpha));
        for(int l=0;l<=16;l++) {
            float a=l*((float)Math.PI*2F)/16F, sn=(float)Math.sin(a), cs=(float)Math.cos(a);
            out.add(new FanVertex(sn*120F,cs*120F,-cs*40F*alpha,0F));
        }
        return List.copyOf(out);
    }
}
