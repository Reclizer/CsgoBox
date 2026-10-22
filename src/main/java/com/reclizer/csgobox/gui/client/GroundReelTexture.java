package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.reclizer.csgobox.block.PlacedBoxEntity;
import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.util.*;

/** One small reusable target, drawn immediately per case; preserves the world's framebuffer and matrices. */
final class GroundReelTexture {
    static final int WIDTH=512, HEIGHT=128;
    private record Visual(long seed,List<DisplayReward> cards,List<CardItemLayout> layouts,ReelMotion motion) {}
    private static final Map<PlacedBoxEntity,Visual> CACHE=new WeakHashMap<>();
    private static final MultiBufferSource.BufferSource GUI_BUFFERS=MultiBufferSource.immediate(new BufferBuilder(256*1024));
    private static TextureTarget target;
    private GroundReelTexture() {}
    static void forget(PlacedBoxEntity box) { CACHE.remove(box); }
    static void clear() {
        CACHE.clear(); if (target!=null) { target.destroyBuffers(); target=null; }
    }
    private static Visual visual(PlacedBoxEntity box) {
        var existing=CACHE.get(box);
        if (existing!=null && existing.seed()==box.visualSeed()) return existing;
        var random=new Random(box.visualSeed()); var cards=new ArrayList<DisplayReward>();
        var samples=box.samples().isEmpty()?List.of(box.winner()):box.samples();
        for (int i=0;i<ReelMotion.CARD_COUNT;i++) cards.add(samples.get(random.nextInt(samples.size())));
        cards.set(ReelMotion.WINNER,box.winner());
        var cached=new IdentityHashMap<net.minecraft.world.item.ItemStack,CardItemLayout>();
        var layouts=cards.stream().map(card->cached.computeIfAbsent(card.stack(),CardItemLayout::of)).toList();
        var result=new Visual(box.visualSeed(),List.copyOf(cards),layouts,GroundAnimation.motion(box.visualSeed(),samples.size()));
        CACHE.put(box,result); return result;
    }
    static void render(PlacedBoxEntity box,float partial,Matrix4f worldPose,Matrix4f levelLighting) {
        var minecraft=Minecraft.getInstance(); var visual=visual(box);
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting=RenderSystem.getVertexSorting();
        int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        float[] color=RenderSystem.getShaderColor().clone();
        var modelView=RenderSystem.getModelViewStack(); modelView.pushPose();
        try {
            if (target==null) { target=new TextureTarget(WIDTH,HEIGHT,true,Minecraft.ON_OSX); target.setClearColor(.025F,.03F,.04F,1); }
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,WIDTH,HEIGHT,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity(); modelView.translate(0,0,-11000); RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1,1,1,1); RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
            var g=new GuiGraphics(minecraft,GUI_BUFFERS);
            g.fill(0,0,WIDTH,HEIGHT,0xFF0D1118);
            float elapsed=box.spinTicks(partial);
            for (int i=0;i<visual.cards().size();i++) {
                int x=Math.round(visual.motion().left(i,elapsed)*WIDTH), width=Math.round(ReelMotion.CARD_WIDTH*WIDTH);
                if (x+width<0 || x>WIDTH) continue;
                var card=visual.cards().get(i);
                ClassicBoxUi.rewardCard(g,card.stack(),card.grade(),x,10,width,108,visual.layouts().get(i),i!=ReelMotion.WINNER);
            }
            g.pose().pushPose(); g.pose().translate(0,0,400);
            g.fill(WIDTH/2-1,3,WIDTH/2+2,HEIGHT-3,0xFFFFDE69); g.pose().popPose(); g.flush();
        } finally {
            GUI_BUFFERS.endBatch();
            modelView.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw); GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]); Lighting.setupLevel(levelLighting);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader); RenderSystem.setShaderTexture(0,target.getColorTextureId());
        RenderSystem.setShaderColor(1,1,1,1); RenderSystem.disableCull(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        try {
            var vertices=Tesselator.getInstance().getBuilder(); vertices.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            vertices.vertex(worldPose,0,HEIGHT,0).uv(0,0).color(255,255,255,255).endVertex();
            vertices.vertex(worldPose,WIDTH,HEIGHT,0).uv(1,0).color(255,255,255,255).endVertex();
            vertices.vertex(worldPose,WIDTH,0,0).uv(1,1).color(255,255,255,255).endVertex();
            vertices.vertex(worldPose,0,0,0).uv(0,1).color(255,255,255,255).endVertex();
            BufferUploader.drawWithShader(vertices.end());
        } finally {
            RenderSystem.enableCull(); RenderSystem.defaultBlendFunc(); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        }
    }
}
