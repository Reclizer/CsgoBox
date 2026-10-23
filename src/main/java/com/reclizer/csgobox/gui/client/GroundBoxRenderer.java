package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.BoxAppearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original baked geometry and hinged lid, with a raised reel tilted towards the case front. */
public final class GroundBoxRenderer implements BlockEntityRenderer<PlacedBoxEntity> {
    public GroundBoxRenderer(BlockEntityRendererProvider.Context context) {}
    private static ResourceLocation part(BoxAppearance appearance,String part) {
        return new ResourceLocation(CsgoBox.MODID,"block/animated/"+appearance.id()+"_"+part);
    }
    private static void mesh(PoseStack pose,MultiBufferSource buffers,PlacedBoxEntity box,BoxAppearance appearance,
                             String part,int light,int overlay) {
        var minecraft=Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),box.getBlockState(),
                minecraft.getModelManager().getModel(part(appearance,part)),1,1,1,light,overlay);
    }
    @Override public void render(PlacedBoxEntity box,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var appearance=box.getBlockState().getValue(PlacedBoxBlock.APPEARANCE);
        pose.pushPose();
        try {
            pose.translate(.5,0,.5);
            pose.mulPose(Axis.YP.rotationDegrees(180-box.getBlockState().getValue(PlacedBoxBlock.FACING).toYRot()));
            pose.translate(-.5,0,-.5);
            mesh(pose,buffers,box,appearance,"body",light,overlay);
            pose.pushPose();
            pose.translate(.5,appearance.hingeY(),appearance.hingeZ());
            pose.mulPose(Axis.XP.rotationDegrees(90*box.lidProgress(partial)));
            pose.translate(-.5,-appearance.hingeY(),-appearance.hingeZ());
            mesh(pose,buffers,box,appearance,"lid",light,overlay);
            pose.popPose();
            boolean shadow=GroundShaderCompatibility.shadowPass();
            if (box.isOpening() && !shadow) GroundReelTexture.request(box);
            if (box.isOpening() && !shadow && box.spinTicks(partial)>=0 && Minecraft.getInstance().gameRenderer.getMainCamera()
                    .getPosition().distanceToSqr(box.getBlockPos().getCenter())<24*24) {
                pose.pushPose();
                pose.translate(.5,appearance.hingeY()+.12,(appearance.front()+appearance.back())/32);
                // 30 degrees above horizontal: rear/top edge higher than front/bottom edge.
                pose.mulPose(Axis.XP.rotationDegrees(-120));
                pose.scale(-.80F/GroundReelTexture.WIDTH,.26F/GroundReelTexture.HEIGHT,.001F);
                pose.translate(-GroundReelTexture.WIDTH/2F,-GroundReelTexture.HEIGHT/2F,0);
                GroundReelTexture.render(box,pose.last(),buffers,overlay);
                pose.popPose();
            } else if (!box.isOpening()) GroundReelTexture.forget(box);
        } finally { pose.popPose(); }
    }
    public static void clear() { GroundReelTexture.clear(); }
    @Mod.EventBusSubscriber(modid=CsgoBox.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
            for (var appearance:BoxAppearance.values()) for (String part:new String[]{"body","lid"}) event.register(part(appearance,part));
        }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlocks.BOX_ENTITY.get(),GroundBoxRenderer::new);
        }
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener)manager->clear());
        }
    }
}
