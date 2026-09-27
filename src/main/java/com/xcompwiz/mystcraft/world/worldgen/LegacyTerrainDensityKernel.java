package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Registry-independent compatibility density kernels for Mystcraft 0.13.7.06 terrain behavior.
 * These deliberately do not write chunks; they are safe to unit-check without a
 * Minecraft runtime and can later be connected to the 1.21.1 ChunkGenerator bridge.
 */
public final class LegacyTerrainDensityKernel {
    private LegacyTerrainDensityKernel() {}

    public static final class Normal {
        private final boolean amplified;
        private final LegacyNoiseGeneratorOctaves3D noise1;
        private final LegacyNoiseGeneratorOctaves3D noise2;
        private final LegacyNoiseGeneratorOctaves3D noise3;
        private final LegacyNoiseGeneratorOctaves3D noise4;
        private final LegacyNoiseGeneratorOctaves3D noise5;
        private final float[] parabolic = new float[25];

        public Normal(long seed, boolean amplified) {
            this.amplified = amplified;
            Random r = new Random(seed);
            noise1 = new LegacyNoiseGeneratorOctaves3D(r, 16);
            noise2 = new LegacyNoiseGeneratorOctaves3D(r, 16);
            noise3 = new LegacyNoiseGeneratorOctaves3D(r, 8);
            noise4 = new LegacyNoiseGeneratorOctaves3D(r, 10);
            noise5 = new LegacyNoiseGeneratorOctaves3D(r, 16);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                parabolic[(z + 2) * 5 + x + 2] = (float)(10.0 / Math.sqrt(x * x + z * z + 0.2F));
        }

        /**
         * @param legacyBiomeIds legacy biome-id window of (sizeX+5)*(sizeZ+5), in old BiomeProvider order.
         */
        public double[] generate(int subX, int subY, int subZ, int sizeX, int sizeY, int sizeZ,
                                 int[] legacyBiomeIds) {
            int biomeWidth = sizeX + 5;
            if (legacyBiomeIds.length < biomeWidth * (sizeZ + 5))
                throw new IllegalArgumentException("legacy biome window too small");
            double[] out = new double[sizeX * sizeY * sizeZ];
            double c1 = 684.412D, c2 = 684.412D;
            // Old code evaluates noise4 even though it never consumes the result.
            noise4.generate(null, subX, 10, subZ, sizeX, 1, sizeZ, 1.121D, 1D, 1.121D);
            double[] n5 = noise5.generate(null, subX, 10, subZ, sizeX, 1, sizeZ, 200D, 1D, 200D);
            double[] n3 = noise3.generate(null, subX, subY, subZ, sizeX, sizeY, sizeZ, c1 / 80D, c2 / 160D, c1 / 80D);
            double[] n1 = noise1.generate(null, subX, subY, subZ, sizeX, sizeY, sizeZ, c1, c2, c1);
            double[] n2 = noise2.generate(null, subX, subY, subZ, sizeX, sizeY, sizeZ, c1, c2, c1);
            int ni = 0, n5i = 0;
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    LegacyBiomeTerrainProfile center = LegacyBiomeTerrainProfile.require(
                            legacyBiomeIds[x + 2 + (z + 2) * biomeWidth]);
                    float avgVar = 0, avgBase = 0, total = 0;
                    for (int xo = -2; xo <= 2; xo++) {
                        for (int zo = -2; zo <= 2; zo++) {
                            LegacyBiomeTerrainProfile p = LegacyBiomeTerrainProfile.require(
                                    legacyBiomeIds[x + xo + 2 + (z + zo + 2) * biomeWidth]);
                            float base = (float)p.baseHeight();
                            float variation = (float)p.heightVariation();
                            if (amplified && base > 0) {
                                base = 1F + base * 2F;
                                variation = 1F + variation * 4F;
                            }
                            float weight = parabolic[xo + 2 + (zo + 2) * 5] / (base + 2F);
                            if (base > center.baseHeight()) weight /= 2F;
                            avgVar += variation * weight;
                            avgBase += base * weight;
                            total += weight;
                        }
                    }
                    avgVar = avgVar / total * .9F + .1F;
                    avgBase = (avgBase / total * 4F - 1F) / 8F;
                    double q = n5[n5i++] / 8000D;
                    if (q < 0) q = -q * .3D;
                    q = q * 3D - 2D;
                    if (q < 0) {
                        q /= 2D;
                        if (q < -1D) q = -1D;
                        q /= 1.4D;
                        q /= 2D;
                    } else {
                        if (q > 1D) q = 1D;
                        q /= 8D;
                    }
                    for (int y = 0; y < sizeY; y++) {
                        double minH = avgBase + q * .2D;
                        minH = minH * sizeY / 16D;
                        double h = sizeY / 2D + minH * 4D;
                        double avgDensity = ((y - h) * 12D * 128D) / 128D / avgVar;
                        if (avgDensity < 0) avgDensity *= 4D;
                        double a = n1[ni] / 512D;
                        double b = n2[ni] / 512D;
                        double mix = (n3[ni] / 10D + 1D) / 2D;
                        double density = mix < 0 ? a : mix > 1 ? b : a + (b - a) * mix;
                        density -= avgDensity;
                        if (y > sizeY - 4) {
                            double f = (y - (sizeY - 4)) / 3F;
                            density = density * (1D - f) - 10D * f;
                        }
                        out[ni++] = density;
                    }
                }
            }
            return out;
        }
    }

    public static final class Nether {
        private final LegacyNoiseGeneratorOctaves3D n1, n2, n3, n4, n5;
        public Nether(long seed) {
            Random r = new Random(seed);
            n1 = new LegacyNoiseGeneratorOctaves3D(r,16); n2 = new LegacyNoiseGeneratorOctaves3D(r,16);
            n3 = new LegacyNoiseGeneratorOctaves3D(r,8); n4 = new LegacyNoiseGeneratorOctaves3D(r,10);
            n5 = new LegacyNoiseGeneratorOctaves3D(r,16);
        }
        public double[] generate(int sx,int sy,int sz,int xs,int ys,int zs) {
            double c1=684.41200000000003D,c2=2053.2359999999999D;
            double[] a4=n4.generate(null,sx,sy,sz,xs,1,zs,1D,0D,1D);
            double[] a5=n5.generate(null,sx,sy,sz,xs,1,zs,100D,0D,100D);
            double[] a3=n3.generate(null,sx,sy,sz,xs,ys,zs,c1/80D,c2/60D,c1/80D);
            double[] a1=n1.generate(null,sx,sy,sz,xs,ys,zs,c1,c2,c1);
            double[] a2=n2.generate(null,sx,sy,sz,xs,ys,zs,c1,c2,c1);
            double[] out=new double[xs*ys*zs], curve=new double[ys];
            for(int y=0;y<ys;y++){ curve[y]=Math.cos(y*Math.PI*6D/ys)*2D; double d=y>ys/2?ys-1-y:y; if(d<4){d=4-d;curve[y]-=d*d*d*10D;}}
            int i=0,j=0;
            for(int x=0;x<xs;x++) for(int z=0;z<zs;z++){
                double unused=(a4[j]+256D)/512D; if(unused>1)unused=1;
                double q=a5[j]/8000D; if(q<0)q=-q; q=q*3D-3D;
                if(q<0){q/=2D;if(q<-1)q=-1;q/=1.3999999999999999D;q/=2D;unused=0D;} else {if(q>1)q=1;q/=6D;}
                unused+=.5D; q=q*ys/16D; j++;
                for(int y=0;y<ys;y++){
                    double aa=a1[i]/512D,bb=a2[i]/512D,m=(a3[i]/10D+1D)/2D;
                    double d=m<0?aa:m>1?bb:aa+(bb-aa)*m; d-=curve[y];
                    if(y>ys-4){double f=(y-(ys-4))/3F;d=d*(1-f)-10D*f;}
                    out[i++]=d;
                }
            }
            return out;
        }
    }

    public static final class End {
        private final LegacyNoiseGeneratorOctaves3D n1,n2,n3,n4,n5;
        public End(long seed){Random r=new Random(seed);n1=new LegacyNoiseGeneratorOctaves3D(r,16);n2=new LegacyNoiseGeneratorOctaves3D(r,16);n3=new LegacyNoiseGeneratorOctaves3D(r,8);n4=new LegacyNoiseGeneratorOctaves3D(r,10);n5=new LegacyNoiseGeneratorOctaves3D(r,16);}
        public double[] generate(int sx,int sy,int sz,int xs,int ys,int zs){
            double v8=684.412D,v10=684.412D;
            double[] a4=n4.generate2D(null,sx,sz,xs,zs,1.121D,1.121D);
            double[] a5=n5.generate2D(null,sx,sz,xs,zs,200D,200D);
            v8*=2D;
            double[] a1=n3.generate(null,sx,sy,sz,xs,ys,zs,v8/80D,v10/160D,v8/80D);
            double[] a2=n1.generate(null,sx,sy,sz,xs,ys,zs,v8,v10,v8);
            double[] a3=n2.generate(null,sx,sy,sz,xs,ys,zs,v8,v10,v8);
            double[] out=new double[xs*ys*zs]; int i=0,j=0;
            for(int x=0;x<xs;x++)for(int z=0;z<zs;z++){
                double q4=(a4[j]+256D)/512D;if(q4>1)q4=1;
                double q5=a5[j]/8000D;if(q5<0)q5=-q5*.3D;q5=q5*3D-2D;
                float xx=x+sx,zz=z+sz;float dist=100F-(float)Math.sqrt(xx*xx+zz*zz)*4F;if(dist>80)dist=80;if(dist<-100)dist=-100;
                if(q5>1)q5=1;q5/=8D;q5=0D;if(q4<0)q4=0;q4+=.5D;q5=q5*ys/16D;j++;
                for(int y=0;y<ys;y++){
                    double aa=a2[i]/512D,bb=a3[i]/512D,m=(a1[i]/10D+1D)/2D;
                    double d=m<0?aa:m>1?bb:aa+(bb-aa)*m;d-=8D;d+=dist;
                    int value=2;if(y>ys/2-value){double f=(y-(ys/2-value))/64F;if(f<0)f=0;if(f>1)f=1;d=d*(1-f)-3000D*f;}
                    value=8;if(y<value){double f=(value-y)/(value-1F);d=d*(1-f)-30D*f;}
                    out[i++]=d;
                }
            }
            return out;
        }
    }
}
