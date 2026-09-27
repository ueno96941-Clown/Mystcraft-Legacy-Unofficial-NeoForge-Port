package com.xcompwiz.mystcraft.api.world.logic;
import com.xcompwiz.mystcraft.api.util.Color;
import com.xcompwiz.mystcraft.api.util.ColorGradient;
import java.util.List;
/** Legacy modifier wrapper. Dangling instability values contribute to the restored Instability score. */
public final class Modifier {
    private Object value; public int dangling; public static final int dangling_default=100;
    public Modifier(){this(null);} public Modifier(Object value){this(value,dangling_default);} public Modifier(Object value,int dangling){this.value=value;this.dangling=dangling;}
    public Object asObject(){return value;} public Number asNumber(){return value instanceof Number n?n:null;} public Color asColor(){return value instanceof Color c?c:null;}
    public ColorGradient asGradient(){return value instanceof ColorGradient g?g:null;}
    @SuppressWarnings("unchecked") public <T> List<T> asList(){return value instanceof List<?> l?(List<T>)l:null;}
}
