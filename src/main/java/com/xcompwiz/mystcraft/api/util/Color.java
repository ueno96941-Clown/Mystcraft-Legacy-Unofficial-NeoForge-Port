package com.xcompwiz.mystcraft.api.util;
public final class Color {
    public final float r,g,b;
    public Color(float r,float g,float b){this.r=r;this.g=g;this.b=b;}
    public Color(int color){this((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);}
    public Color(java.awt.Color color){this(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f);}
    public Color average(Color o){return new Color((r+o.r)/2,(g+o.g)/2,(b+o.b)/2);}
    public Color average(float r,float g,float b){return new Color((this.r+r)/2,(this.g+g)/2,(this.b+b)/2);}
    public int asInt(){return ((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);}
    public java.awt.Color toAWT(){return new java.awt.Color(r,g,b);}
    @Override public String toString(){return String.format("Color component: (%.2f, %.2f, %.2f)",r,g,b);}
}
