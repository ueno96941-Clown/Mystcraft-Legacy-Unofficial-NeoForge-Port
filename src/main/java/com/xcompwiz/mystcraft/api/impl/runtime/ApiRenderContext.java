package com.xcompwiz.mystcraft.api.impl.runtime;

import com.xcompwiz.mystcraft.api.util.Color;
import net.minecraft.resources.ResourceLocation;

/** Thread-local bridge replacing the old immediate-mode OpenGL RenderAPI. */
public final class ApiRenderContext {
    public interface Renderer {
        void drawWord(float x,float y,float z,float scale,String word);
        void drawSymbol(float x,float y,float z,float scale,ResourceLocation symbol);
        void drawColorEye(float x,float y,float z,float radius,Color color);
    }
    private static final ThreadLocal<Renderer> CURRENT = new ThreadLocal<>();
    private ApiRenderContext() {}
    public static void push(Renderer renderer){ CURRENT.set(renderer); }
    public static void pop(){ CURRENT.remove(); }
    public static Renderer current(){ return CURRENT.get(); }
}
