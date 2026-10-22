package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

/** Restores the actual world state after rendering GUI items inside a block renderer. */
final class WorldGuiRenderState implements AutoCloseable {
    private final Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
    private final Matrix4f textureMatrix=new Matrix4f(RenderSystem.getTextureMatrix());
    private final VertexSorting sorting=RenderSystem.getVertexSorting();
    private final ShaderInstance shader=RenderSystem.getShader();
    private final float[] color=RenderSystem.getShaderColor().clone(), fogColor=RenderSystem.getShaderFogColor().clone();
    private final float fogStart=RenderSystem.getShaderFogStart(), fogEnd=RenderSystem.getShaderFogEnd();
    private final com.mojang.blaze3d.shaders.FogShape fogShape=RenderSystem.getShaderFogShape();
    private final float glintAlpha=RenderSystem.getShaderGlintAlpha();
    private final int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND), cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST), depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final int[] viewport=new int[4], scissorBox=new int[4], colorMask=new int[4], textures=new int[12], bindings=new int[3];
    private final float[] clearColor=new float[4];
    private final Vector3f light0, light1;

    WorldGuiRenderState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport); GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissorBox);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK,colorMask);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clearColor);
        for (int i=0;i<textures.length;i++) textures[i]=RenderSystem.getShaderTexture(i);
        for (int i=0;i<bindings.length;i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0+i); bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        RenderSystem.activeTexture(activeTexture);
        // Copy the live vectors via the public shader uniforms, not a reconstructed camera/block matrix.
        var litShader=GameRenderer.getRendertypeEntityCutoutShader(); RenderSystem.setupShaderLights(litShader);
        light0=vector(litShader.LIGHT0_DIRECTION); light1=vector(litShader.LIGHT1_DIRECTION);
        RenderSystem.getModelViewStack().pushPose();
    }
    private static Vector3f vector(Uniform uniform) {
        var values=uniform.getFloatBuffer(); return new Vector3f(values.get(0),values.get(1),values.get(2));
    }
    void restoreWorldTarget() {
        var modelView=RenderSystem.getModelViewStack(); modelView.popPose(); modelView.pushPose();
        RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw); GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
    }
    @Override public void close() {
        RenderSystem.getModelViewStack().popPose(); RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection,sorting); RenderSystem.setTextureMatrix(textureMatrix);
        RenderSystem.setShaderLights(light0,light1);
        RenderSystem.setShaderFogStart(fogStart); RenderSystem.setShaderFogEnd(fogEnd); RenderSystem.setShaderFogShape(fogShape);
        RenderSystem.setShaderFogColor(fogColor[0],fogColor[1],fogColor[2],fogColor[3]); RenderSystem.setShaderGlintAlpha(glintAlpha);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw); GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
        RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        RenderSystem.clearColor(clearColor[0],clearColor[1],clearColor[2],clearColor[3]);
        for (int i=0;i<textures.length;i++) RenderSystem.setShaderTexture(i,textures[i]);
        for (int i=0;i<bindings.length;i++) { RenderSystem.activeTexture(GL13.GL_TEXTURE0+i); RenderSystem.bindTexture(bindings[i]); }
        RenderSystem.activeTexture(activeTexture);
        RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
        if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.depthFunc(depthFunc); RenderSystem.depthMask(depthMask);
        RenderSystem.colorMask(colorMask[0]!=0,colorMask[1]!=0,colorMask[2]!=0,colorMask[3]!=0);
        RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
        if (!scissor) RenderSystem.disableScissor();
        RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]); RenderSystem.setShader(()->shader);
    }
}
