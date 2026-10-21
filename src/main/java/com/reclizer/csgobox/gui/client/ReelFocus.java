package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.reclizer.csgobox.utils.ReelPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** The original circular spotlight, with a narrow feather instead of a hard texture edge. */
final class ReelFocus {
    private static final int SEGMENTS = 128, FEATHER_STEPS = 8;
    private static final float[] COS = new float[SEGMENTS + 1], SIN = new float[SEGMENTS + 1];
    static {
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = i * Math.PI * 2 / SEGMENTS;
            COS[i] = (float) Math.cos(angle);
            SIN[i] = (float) Math.sin(angle);
        }
        COS[SEGMENTS] = COS[0];
        SIN[SEGMENTS] = SIN[0];
    }
    private ReelFocus() {}

    static void render(GuiGraphics g, int width, int height, float opacity) {
        if (opacity <= 0) return;
        g.flush();
        // Match the old 2000 x 1124 mask's circle; both axes use the same logical radius.
        float cx = width * 0.501F, cy = height * 0.483F;
        float radius = height * 0.308F, feather = height * 0.022F;
        float inner = radius - feather / 2;
        float outer = (float) Math.hypot(width, height);
        Matrix4f pose = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int ring = 0; ring <= FEATHER_STEPS; ring++) {
                float r0 = inner + feather * ring / FEATHER_STEPS;
                float r1 = ring == FEATHER_STEPS ? outer : inner + feather * (ring + 1) / FEATHER_STEPS;
                int a0 = Math.round(158 * opacity * ReelPresentation.smooth((float) ring / FEATHER_STEPS));
                int a1 = Math.round(158 * opacity * ReelPresentation.smooth((ring + 1F) / FEATHER_STEPS));
                for (int i = 0; i < SEGMENTS; i++) {
                    vertex(buffer, pose, cx, cy, r0, i, a0);
                    vertex(buffer, pose, cx, cy, r0, i + 1, a0);
                    vertex(buffer, pose, cx, cy, r1, i + 1, a1);
                    vertex(buffer, pose, cx, cy, r1, i, a1);
                }
            }
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float cx, float cy, float radius,
                               int index, int alpha) {
        buffer.vertex(pose, cx + radius * COS[index], cy + radius * SIN[index], 300)
                .color(29, 29, 28, alpha).endVertex();
    }
}
