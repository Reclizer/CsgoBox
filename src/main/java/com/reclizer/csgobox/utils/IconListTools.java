package com.reclizer.csgobox.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public class IconListTools {


//    public  void renderItem(GuiGraphics gui, ItemStack itemStack, int pX, int pY,float scale) {
//        // Based on Integrated Dynamics's ItemValueTypeWorldRenderer
//        renderGuiItem(gui.pose(), itemStack, pX, pY, scale);
//        Lighting.setupFor3DItems();
//    }

    public static void renderRarity(GuiGraphics guiGraphics,int pX0,int pY0,int toX,int toY,int color){


        guiGraphics.fillGradient(pX0,pY0,toX,toY,0xFF696969,0xFFD3D3D3);
        guiGraphics.fill(pX0,pY0,pX0+2,toY,color);
    }
    public  static  void  renderItemFrame(LivingEntity entity ,GuiGraphics guiGraphics,ItemStack itemStack,int pX,int pY,int width,int height,int grade){
        int color=ColorTools.colorItems(grade);

        int FrameWidth=width*8/100;
        int FrameHeight=height*11/100;
        float scale=FrameWidth*60F/100F/16F;
        int toX=pX+FrameWidth;
        int toY=pY+FrameHeight;
        int itemX=pX+FrameWidth*20/100;
        int itemY=pY+FrameHeight*10/100;
        if(grade==5){
            guiGraphics.fillGradient(pX,pY,toX,toY,0xFF533c00,0xFFb69008);
            guiGraphics.fill(pX,pY,pX+2,toY,color);
            guiGraphics.blit(new ResourceLocation("csgobox:textures/screens/gold_item.png"), pX+2, pY+2, 0, 0, FrameWidth-4, FrameHeight-4, FrameWidth-4, FrameHeight-4);
        }else {
            renderRarity(guiGraphics,pX,pY,toX,toY,color);
            renderGuiItem(entity,entity.level(),guiGraphics,itemStack,itemX, itemY, scale);
        }

    }

    public static void renderGuiItem(LivingEntity entity, Level world, GuiGraphics guiGraphics, ItemStack itemStack, float pX, float pY, float scale) {

        renderGuiItem(guiGraphics.pose(), itemStack, pX, pY, Minecraft.getInstance().getItemRenderer().getModel(itemStack, (Level)world, (LivingEntity)entity, 0), scale);
    }
    protected static void renderGuiItem(PoseStack poseStack, ItemStack itemStack, float pX, float pY, BakedModel bakedModel, float scale) {
        poseStack.pushPose();
        //poseStack.scale(scale / 16, scale / 16, 1);
        poseStack.translate((float)pX, (float)pY, 2F);
        poseStack.translate(8.0F*scale, 8.0F*scale, 0.0F);
        poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 0F));
        float f3 =45F;
        //poseStack.mulPose(Axis.XP.rotation(f3));

        poseStack.scale(16.0F*scale, 16.0F*scale, 0);
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean flag = !bakedModel.usesBlockLight();
        if (flag) {
            Lighting.setupForFlatItems();
        }

        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, new PoseStack(), multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
        multibuffersource$buffersource.endBatch();
        RenderSystem.enableDepthTest();
        //RenderSystem.disableDepthTest();
        if (flag) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
    }
    //=======================================================================================================================================================

    public  static  void  renderItemProgress(LivingEntity entity ,GuiGraphics guiGraphics,ItemStack itemStack,float pX,float pY,float width,float height,int grade){
        int color=ColorTools.colorItems(grade);
        float FrameWidth=width*18/100;
        float FrameHeight=height*25/100;
        float scale=FrameWidth*60F/100F/16F;
        float toX=pX+FrameWidth;
        float toY=pY+FrameHeight;
        float itemX=pX+FrameWidth*20/100;
        float itemY=pY+FrameHeight*10/100;
        if(grade==5){
            fillGradient(guiGraphics,pX,pY,toX,toY,0xFF533c00,0xFFb69008);

            blit(guiGraphics,new ResourceLocation("csgobox:textures/screens/gold_item.png"), pX+2F, pY+2, 0, 0, FrameWidth-4, FrameHeight-4, FrameWidth-4, FrameHeight-4);

            fill(guiGraphics,pX,toY,toX,toY+2,color);
        }else {
            fillGradient(guiGraphics,(float) pX,(float)pY,(float)toX,(float)toY,0xFF696969,0xFFA9A9A9);
            fillGradient(guiGraphics,pX,pY+FrameHeight*2/3,toX,toY,ColorTools.argbColor(0,128,128,128),ColorTools.deepColor(color));

            renderGuiItem(entity,entity.level(),guiGraphics,itemStack,itemX, itemY, scale);
            RenderSystem.enableBlend();
            fill(guiGraphics,pX,toY,toX,toY+2,color);
        }




    }

    //=======================================================================================================================================================
    //fill
    public static void fill(GuiGraphics guiGraphics,float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        float pZ=3;
        RenderType pRenderType=RenderType.gui();

        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float) FastColor.ARGB32.alpha(pColor) / 255.0F;
        float f = (float)FastColor.ARGB32.red(pColor) / 255.0F;
        float f1 = (float)FastColor.ARGB32.green(pColor) / 255.0F;
        float f2 = (float)FastColor.ARGB32.blue(pColor) / 255.0F;
        VertexConsumer vertexconsumer = guiGraphics.bufferSource().getBuffer(pRenderType);
        vertexconsumer.vertex(matrix4f, (float)pMinX, (float)pMinY, (float)pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, (float)pMinX, (float)pMaxY, (float)pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, (float)pMaxX, (float)pMaxY, (float)pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, (float)pMaxX, (float)pMinY, (float)pZ).color(f, f1, f2, f3).endVertex();
        flush(guiGraphics);
    }

    //====================================================================================================================================================
    public static void fillGradient(GuiGraphics guiGraphics,float pX1, float pY1, float pX2, float pY2, int pColorFrom, int pColorTo) {
        //guiGraphics.pose().pushPose();
        float pZ=1;

        RenderType pRenderType=RenderType.gui();

        VertexConsumer pConsumer = guiGraphics.bufferSource().getBuffer(pRenderType);

        float f = (float)FastColor.ARGB32.alpha(pColorFrom) / 255.0F;
        float f1 = (float)FastColor.ARGB32.red(pColorFrom) / 255.0F;
        float f2 = (float)FastColor.ARGB32.green(pColorFrom) / 255.0F;
        float f3 = (float)FastColor.ARGB32.blue(pColorFrom) / 255.0F;
        float f4 = (float)FastColor.ARGB32.alpha(pColorTo) / 255.0F;
        float f5 = (float)FastColor.ARGB32.red(pColorTo) / 255.0F;
        float f6 = (float)FastColor.ARGB32.green(pColorTo) / 255.0F;
        float f7 = (float)FastColor.ARGB32.blue(pColorTo) / 255.0F;

        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        pConsumer.vertex(matrix4f, (float)pX1, (float)pY1, (float)pZ).color(f1, f2, f3, f).endVertex();
        pConsumer.vertex(matrix4f, (float)pX1, (float)pY2, (float)pZ).color(f5, f6, f7, f4).endVertex();
        pConsumer.vertex(matrix4f, (float)pX2, (float)pY2, (float)pZ).color(f5, f6, f7, f4).endVertex();
        pConsumer.vertex(matrix4f, (float)pX2, (float)pY1, (float)pZ).color(f1, f2, f3, f).endVertex();
        //guiGraphics.pose().popPose();

        flush(guiGraphics);
    }




    //====================================================================================================================================================

    public static void blit(GuiGraphics guiGraphics,ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight) {

        float pX1=pX;
        float pX2=pX + pWidth;
        float pY1=pY;
        float pY2=pY + pHeight;
        //深度
        int pBlitOffset=4;

        float pUWidth=pWidth;
        float pVHeight=pHeight;
        float pMinU=(pUOffset + 0.0F) / (float)pTextureWidth;
        float pMaxU=(pUOffset + (float)pUWidth) / (float)pTextureWidth;
        float pMinV=(pVOffset + 0.0F) / (float)pTextureHeight;
        float pMaxV=(pVOffset + (float)pVHeight) / (float)pTextureHeight;

        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, (float)pX1, (float)pY1, (float)pBlitOffset).uv(pMinU, pMinV).endVertex();
        bufferbuilder.vertex(matrix4f, (float)pX1, (float)pY2, (float)pBlitOffset).uv(pMinU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, (float)pX2, (float)pY2, (float)pBlitOffset).uv(pMaxU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, (float)pX2, (float)pY1, (float)pBlitOffset).uv(pMaxU, pMinV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        flush(guiGraphics);
    }

    //====================================================================================================================================================
    public static void flush(GuiGraphics guiGraphics) {
        RenderSystem.disableDepthTest();
        guiGraphics.bufferSource().endBatch();
        RenderSystem.enableDepthTest();
    }
}
