package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xcompwiz.mystcraft.world.worldgen.AgeSkyGeometry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import org.joml.Matrix4f;

/** 1.21.1 immediate uploader for legacy POSITION_COLOR quad meshes. */
public final class AgeSkyGpuUploader {
    private AgeSkyGpuUploader() {}

    public static void drawColored(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, float red,float green,float blue,float alpha) {
        if(quads.isEmpty() || alpha<=0) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for(var q:quads) { put(b,q.a(),pose,red,green,blue,alpha); put(b,q.b(),pose,red,green,blue,alpha);
            put(b,q.c(),pose,red,green,blue,alpha); put(b,q.d(),pose,red,green,blue,alpha); }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }
    private static void put(BufferBuilder b,AgeSkyGeometry.Vertex v,Matrix4f pose,float r,float g,float bl,float a) {
        b.addVertex(pose,v.x(),v.y(),v.z()).setColor(v.r()*r,v.g()*g,v.b()*bl,v.a()*a);
    }

    /** Draws geometry whose vertices already contain the final RGB gradient. */
    public static void drawIntrinsicColored(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, float alpha) {
        if (quads.isEmpty() || alpha <= 0F) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (var q : quads) {
            put(b,q.a(),pose,1F,1F,1F,alpha); put(b,q.b(),pose,1F,1F,1F,alpha);
            put(b,q.c(),pose,1F,1F,1F,alpha); put(b,q.d(),pose,1F,1F,1F,alpha);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }


    public static void drawTexturedQuad(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, ResourceLocation texture, float alpha) {
        drawTexturedRegion(quads, pose, texture, alpha, 0,0,1,1);
    }
    public static void drawTexturedRegion(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, ResourceLocation texture, float alpha,
                                          float u0,float v0,float u1,float v1) {
        if(quads.isEmpty()||alpha<=0)return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1,1,1,alpha);
        BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for(var q:quads) {
            b.addVertex(pose,q.a().x(),q.a().y(),q.a().z()).setUv(u0,v0);
            b.addVertex(pose,q.b().x(),q.b().y(),q.b().z()).setUv(u1,v0);
            b.addVertex(pose,q.c().x(),q.c().y(),q.c().z()).setUv(u1,v1);
            b.addVertex(pose,q.d().x(),q.d().y(),q.d().z()).setUv(u0,v1);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1,1,1,1);
    }

    public static void drawSunsetFan(List<AgeSkyGeometry.FanVertex> fan, Matrix4f pose,
                                     float red,float green,float blue) {
        if(fan.size()<3)return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        for(var v:fan) b.addVertex(pose,v.x(),v.y(),v.z()).setColor(red,green,blue,v.alpha());
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    public static void drawTexturedTinted(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, ResourceLocation texture,
                                          float red,float green,float blue,float alpha,float uvMax) {
        if(quads.isEmpty()||alpha<=0)return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red,green,blue,alpha);
        BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for(var q:quads) {
            b.addVertex(pose,q.a().x(),q.a().y(),q.a().z()).setUv(0,0);
            b.addVertex(pose,q.b().x(),q.b().y(),q.b().z()).setUv(uvMax,0);
            b.addVertex(pose,q.c().x(),q.c().y(),q.c().z()).setUv(uvMax,uvMax);
            b.addVertex(pose,q.d().x(),q.d().y(),q.d().z()).setUv(0,uvMax);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1,1,1,1);
    }

    /** Legacy StarsEndSky maps the same base quad UV orientation onto each rotated cube face. */
    public static void drawEndSkyLegacy(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, ResourceLocation texture,
                                        float red,float green,float blue,float alpha) {
        if(quads.isEmpty()||alpha<=0)return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red,green,blue,alpha);
        BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for(var q:quads) {
            b.addVertex(pose,q.a().x(),q.a().y(),q.a().z()).setUv(0,0);
            b.addVertex(pose,q.b().x(),q.b().y(),q.b().z()).setUv(0,16);
            b.addVertex(pose,q.c().x(),q.c().y(),q.c().z()).setUv(16,16);
            b.addVertex(pose,q.d().x(),q.d().y(),q.d().z()).setUv(16,0);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1,1,1,1);
    }

}
