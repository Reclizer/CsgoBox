package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.reclizer.csgobox.utils.ReelPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** One reusable layer: fade the complete item/card, including custom mod renderers. */
final class ReelLayer implements AutoCloseable {
    private TextureTarget target;

    void render(GuiGraphics g, int width, int height, float opacity, boolean edges, Runnable content) {
        if (opacity <= 0) return;
        var minecraft = Minecraft.getInstance();
        var main = minecraft.getMainRenderTarget();
        g.flush();
        try {
            if (target == null || target.width != main.width || target.height != main.height) {
                close();
                target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                target.setClearColor(0, 0, 0, 0);
            }
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            content.run();
            g.flush();
        } finally {
            main.bindWrite(true);
        }
        // GUI projection is unchanged while drawing into the equally-sized target.
        Matrix4f pose = g.pose().last().pose();
        float guiWidth = minecraft.getWindow().getGuiScaledWidth();
        float guiHeight = minecraft.getWindow().getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        // The off-screen colour is already premultiplied by alpha.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < 48; i++) {
                float x0 = width * i / 48F, x1 = width * (i + 1) / 48F;
                vertex(buffer, pose, x0, height, width, guiWidth, guiHeight, opacity, edges);
                vertex(buffer, pose, x1, height, width, guiWidth, guiHeight, opacity, edges);
                vertex(buffer, pose, x1, 0, width, guiWidth, guiHeight, opacity, edges);
                vertex(buffer, pose, x0, 0, width, guiWidth, guiHeight, opacity, edges);
            }
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float width,
                               float guiWidth, float guiHeight, float opacity, boolean edges) {
        float alpha = opacity * (edges ? ReelPresentation.edgeAlpha(x / width) : 1);
        float shade = alpha * (edges ? ReelPresentation.brightness(x / width) : 1);
        float u = (pose.m00() * x + pose.m10() * y + pose.m30()) / guiWidth;
        float v = 1 - (pose.m01() * x + pose.m11() * y + pose.m31()) / guiHeight;
        buffer.vertex(pose, x, y, 200).uv(u, v).color(shade, shade, shade, alpha).endVertex();
    }

    @Override public void close() {
        if (target != null) { target.destroyBuffers(); target = null; }
    }
}
