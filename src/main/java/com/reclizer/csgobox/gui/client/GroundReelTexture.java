package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.block.PlacedBoxEntity;
import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** GUI targets are prepared before world rendering; block renderers only submit ordinary textured vertices. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID,value=Dist.CLIENT)
public final class GroundReelTexture {
    static final int WIDTH=512, HEIGHT=128;
    private static final int CARD_WIDTH=Math.round(ReelMotion.CARD_WIDTH*WIDTH), COLUMNS=5, ROWS=4;
    private static final long UPDATE_INTERVAL=1_000_000_000L/60;
    private static final Map<PlacedBoxEntity,Visual> CACHE=new LinkedHashMap<>();
    private static final List<Panel> PANELS=new ArrayList<>();
    private static final MultiBufferSource.BufferSource GUI_BUFFERS=MultiBufferSource.immediate(new BufferBuilder(256*1024));
    private static long frame;
    private static final class Visual {
        final long seed;
        final List<Card> cards;
        final List<Card> unique;
        final ReelMotion motion;
        final Panel panel;
        long seen=frame, updated;
        boolean baked;
        Visual(PlacedBoxEntity box,Panel panel) {
            this.seed=box.visualSeed(); this.panel=panel;
            var random=new Random(seed); var cards=new ArrayList<Card>(); var unique=new ArrayList<Card>();
            var samples=box.samples().isEmpty()?List.of(box.winner()):box.samples();
            for (int i=0;i<ReelMotion.CARD_COUNT;i++) {
                var reward=i==ReelMotion.WINNER?box.winner():samples.get(random.nextInt(samples.size()));
                // Keep the same random sequence and landing as the server's reel motion.
                if (i==ReelMotion.WINNER) random.nextInt(samples.size());
                boolean mystery=i!=ReelMotion.WINNER && reward.grade()==5;
                Card card=null;
                for (var existing:unique) if (existing.reward.stack()==reward.stack() && existing.reward.grade()==reward.grade()
                        && existing.mystery==mystery) { card=existing; break; }
                if (card==null) { card=new Card(reward,mystery,CardItemLayout.of(reward.stack()),unique.size()); unique.add(card); }
                cards.add(card);
            }
            this.cards=List.copyOf(cards); this.unique=List.copyOf(unique);
            this.motion=GroundAnimation.motion(seed,samples.size());
        }
    }
    private record Card(DisplayReward reward,boolean mystery,CardItemLayout layout,int slot) {}
    /** Fixed resource names keep RenderType's own caches bounded across repeated openings. */
    private static final class Panel {
        final ResourceLocation id, atlasId;
        TextureTarget target, atlas;
        boolean used;
        Panel(int slot) {
            id=new ResourceLocation(CsgoBox.MODID,"dynamic/ground_reel_"+slot);
            atlasId=new ResourceLocation(CsgoBox.MODID,"dynamic/ground_cards_"+slot);
        }
        void create() {
            if (target!=null) return;
            target=new TextureTarget(WIDTH,HEIGHT,true,Minecraft.ON_OSX); target.setClearColor(.025F,.03F,.04F,1);
            atlas=new TextureTarget(COLUMNS*CARD_WIDTH,ROWS*HEIGHT,true,Minecraft.ON_OSX); atlas.setClearColor(0,0,0,0);
            var textures=Minecraft.getInstance().getTextureManager();
            textures.register(id,new TargetTexture(target)); textures.register(atlasId,new TargetTexture(atlas));
        }
        void close() {
            if (target==null) return;
            var textures=Minecraft.getInstance().getTextureManager(); textures.release(id); textures.release(atlasId);
            target.destroyBuffers(); atlas.destroyBuffers(); target=null; atlas=null;
        }
    }
    private static final class TargetTexture extends AbstractTexture {
        private final TextureTarget target;
        TargetTexture(TextureTarget target) { this.target=target; }
        @Override public int getId() { return target.getColorTextureId(); }
        @Override public void load(ResourceManager resources) {}
        @Override public void releaseId() {} // The framebuffer owns this texture.
    }
    private GroundReelTexture() {}
    static void forget(PlacedBoxEntity box) {
        var old=CACHE.remove(box); if (old!=null) old.panel.used=false;
    }
    static void clear() {
        CACHE.clear();
        if (!PANELS.isEmpty()) try (var state=new WorldGuiRenderState()) {
            for (var panel:PANELS) panel.close(); PANELS.clear();
        }
    }
    private static Visual visual(PlacedBoxEntity box) {
        var existing=CACHE.get(box);
        if (existing!=null && existing.seed==box.visualSeed()) { existing.seen=frame; return existing; }
        forget(box);
        Panel panel=null;
        for (var candidate:PANELS) if (!candidate.used) { panel=candidate; break; }
        if (panel==null) {
            panel=new Panel(PANELS.size()); PANELS.add(panel);
        }
        panel.used=true;
        var result=new Visual(box,panel); CACHE.put(box,result); return result;
    }
    static void request(PlacedBoxEntity box) { visual(box); }
    @SubscribeEvent public static void beforeFrame(TickEvent.RenderTickEvent event) {
        if (event.phase!=TickEvent.Phase.START) return;
        var mc=Minecraft.getInstance(); ++frame;
        if (mc.level==null) { if (!PANELS.isEmpty()) clear(); return; }
        var camera=mc.gameRenderer.getMainCamera().getPosition();
        var iterator=CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry=iterator.next(); var box=entry.getKey(); var visual=entry.getValue();
            if (box.isRemoved() || box.getLevel()!=mc.level || !box.isOpening() || frame-visual.seen>3
                    || camera.distanceToSqr(box.getBlockPos().getCenter())>=24*24) {
                visual.panel.used=false; iterator.remove(); continue;
            }
            long now=System.nanoTime();
            if (now-visual.updated>=UPDATE_INTERVAL) { prepare(box,event.renderTickTime); visual.updated=now; }
        }
        if (CACHE.isEmpty() && !PANELS.isEmpty()) clear();
    }
    /** Called outside the world's geometry/shadow passes, including by the isolated GPU regression checks. */
    static TextureTarget prepare(PlacedBoxEntity box,float partial) {
        var visual=visual(box); if (visual==null) return null;
        try (var state=new WorldGuiRenderState()) {
            RenderSystem.disableScissor(); RenderSystem.depthMask(true); RenderSystem.colorMask(true,true,true,true);
            visual.panel.create();
            RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,WIDTH,HEIGHT,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var modelView=RenderSystem.getModelViewStack(); modelView.setIdentity(); modelView.translate(0,0,-11000); RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1,1,1,1); RenderSystem.enableDepthTest(); RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            FogRenderer.setupNoFog(); Lighting.setupFor3DItems();
            if (!visual.baked) {
                var atlas=visual.panel.atlas;
                atlas.clear(Minecraft.ON_OSX); atlas.bindWrite(true);
                RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,atlas.width,atlas.height,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
                try {
                    var g=new GuiGraphics(Minecraft.getInstance(),GUI_BUFFERS);
                    for (var card:visual.unique) ClassicBoxUi.rewardCard(g,card.reward.stack(),card.reward.grade(),
                            card.slot%COLUMNS*CARD_WIDTH,card.slot/COLUMNS*HEIGHT+10,CARD_WIDTH,108,card.layout,card.mystery);
                    g.flush();
                } finally { GUI_BUFFERS.endBatch(); }
                visual.baked=true;
            }
            var target=visual.panel.target; target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,WIDTH,HEIGHT,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            try {
                var g=new GuiGraphics(Minecraft.getInstance(),GUI_BUFFERS); g.fill(0,0,WIDTH,HEIGHT,0xFF0D1118);
                float elapsed=box.spinTicks(partial);
                for (int i=0;i<visual.cards.size();i++) {
                    int x=Math.round(visual.motion.left(i,elapsed)*WIDTH);
                    if (x+CARD_WIDTH<0 || x>WIDTH) continue;
                    var card=visual.cards.get(i);
                    // Framebuffer textures have the opposite vertical orientation to ordinary GUI textures.
                    g.blit(visual.panel.atlasId,x,0,CARD_WIDTH,HEIGHT,card.slot%COLUMNS*CARD_WIDTH,
                            (ROWS-1-card.slot/COLUMNS)*HEIGHT+HEIGHT,CARD_WIDTH,-HEIGHT,COLUMNS*CARD_WIDTH,ROWS*HEIGHT);
                }
                g.pose().pushPose(); g.pose().translate(0,0,400);
                g.fill(WIDTH/2-1,3,WIDTH/2+2,HEIGHT-3,0xFFFFDE69); g.pose().popPose(); g.flush();
            } finally { GUI_BUFFERS.endBatch(); }
            return target;
        }
    }
    static void render(PlacedBoxEntity box,PoseStack.Pose pose,MultiBufferSource buffers,int worldLight,int overlay) {
        var visual=CACHE.get(box);
        if (visual==null || !visual.baked) return;
        visual.seen=frame;
        // A shader pack applies its own world lighting. Full block+sky light is amplified again,
        // washing the GUI out; retain local skylight and only a soft, bounded readability floor.
        int light=GroundShaderCompatibility.shaderPackInUse()
                ?LightTexture.pack(Math.max(6,Math.min(10,LightTexture.block(worldLight))),LightTexture.sky(worldLight))
                :LightTexture.FULL_BRIGHT;
        var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(visual.panel.id));
        vertices.vertex(pose.pose(),0,HEIGHT,0).color(255,255,255,255).uv(0,0).overlayCoords(overlay).uv2(light).normal(pose.normal(),0,0,-1).endVertex();
        vertices.vertex(pose.pose(),WIDTH,HEIGHT,0).color(255,255,255,255).uv(1,0).overlayCoords(overlay).uv2(light).normal(pose.normal(),0,0,-1).endVertex();
        vertices.vertex(pose.pose(),WIDTH,0,0).color(255,255,255,255).uv(1,1).overlayCoords(overlay).uv2(light).normal(pose.normal(),0,0,-1).endVertex();
        vertices.vertex(pose.pose(),0,0,0).color(255,255,255,255).uv(0,1).overlayCoords(overlay).uv2(light).normal(pose.normal(),0,0,-1).endVertex();
    }
}
