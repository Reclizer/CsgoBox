package com.reclizer.csgobox.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

public class GuiItemMove {


//    public static void renderEntityInInventoryFollowsMouse(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, float pMouseX, float pMouseY, Item item, LivingEntity player) {
//        ItemStack itemStack=new ItemStack(item);
//
//        ItemEntity itemEntity=new ItemEntity(player.level(),player.position().x,player.position().y,player.position().z,itemStack);
//        //itemEntity.age
//        renderEntityInInventoryFollowsMouse(pGuiGraphics,pX,pY,pScale,pMouseX,pMouseY,itemEntity);
//    }

    public  static  float renderRotAngleY(double pMouse,float itemRot){
        float f1 = (float)Math.atan((double)(pMouse / 40.0F));
        float angle =f1+itemRot;

        if(angle>1.5F){
            angle=1.5F;
        }else if(angle<-1.5F){
            angle=-1.5F;
        }
        return angle;
    }

    public  static  float renderRotAngleX(double pMouse,float itemRot){
        float f1 = (float)Math.atan((double)(pMouse / 40.0F));
        float angle =f1+itemRot;
        //float angle =f1;
        if(angle>3F){
            angle=3F;
        }else if(angle<-3F){
            angle=-3F;
        }
        return angle;
    }




    public static void renderItemInInventoryFollowsMouse(GuiGraphics pGuiGraphics, int pX, int pY,  float angleXComponent, float angleYComponent, ItemStack item,LivingEntity player,float scale) {

        //ItemStack itemStack=new ItemStack(item);

        BakedModel bakedModel=Minecraft.getInstance().getItemRenderer().getModel(item, (Level)player.level(), (LivingEntity)player,0);

        renderItemInInventoryFollowsAngle(pGuiGraphics, pX, pY, angleXComponent, angleYComponent, item,bakedModel,scale);
    }

    public static void renderItemInInventoryFollowsAngle(GuiGraphics pGuiGraphics, int pX, int pY, float angleXComponent, float angleYComponent, ItemStack itemStack,BakedModel bakedModel,float scale) {


        float f = angleXComponent;
        float f1 = angleYComponent;
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
        //Quaternionf quaternionf = (new Quaternionf()).rotateX(f * 20.0F * ((float)Math.PI / 180F));
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(f1 * 20.0F * ((float)Math.PI / 180F));
        quaternionf.mul(quaternionf1);
        //float f2 = pEntity.yBodyRot;
        //float f3 = pEntity.getYRot();
        //float f4 = pEntity.getXRot();
        //float f5 = pEntity.yHeadRotO;
        //float f6 = pEntity.yHeadRot;
        //pEntity.yBodyRot = 180.0F + f * 20.0F;
        //pEntity.setYRot(180.0F + f * 40.0F);
        // pEntity.setXRot(-f1 * 20.0F);
        //pEntity.setYRot(0F);
        //pEntity.setXRot(0F);
        //pEntity.yHeadRot = pEntity.getYRot();
        //pEntity.yHeadRotO = pEntity.getYRot();
        //renderItemInInventory(pGuiGraphics.pose(),itemStack, pX, pY, bakedModel,quaternionf, quaternionf1, 1F);
        renderItemInInventory(pGuiGraphics.pose(),itemStack, pX, pY, bakedModel,f, f1, scale);
        //pEntity.yBodyRot = f2;
        //pEntity.setYRot(f3);
        //pEntity.setXRot(f4);
        //pEntity.yHeadRotO = f5;
        //pEntity.yHeadRot = f6;
    }

//    public static void renderItemInInventory(GuiGraphics guiGraphics, int pX, int pY, Quaternionf pPose, @Nullable Quaternionf pCameraOrientation, ItemStack stack,BakedModel bakedModel){
//
//    }




    protected static void renderItemInInventory(PoseStack poseStack, ItemStack itemStack, int pX, int pY, BakedModel bakedModel,float angleXComponent, float angleYComponent ,float scale) {
        poseStack.pushPose();

        poseStack.translate((float)pX, (float)pY, 100.0F);
        poseStack.translate(8.0F*scale, 8.0F*scale, 0.0F);
        poseStack.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));

//        float rx =45F;
//        float ry =45F;
//        float angleLimit = 40F;
//        if(angleXComponent>angleLimit){
//            angleXComponent=angleLimit;
//        }
//        if(angleXComponent<-angleLimit){
//            angleXComponent=-angleLimit;
//        }
//        float angleLimit=0f;
//        System.out.println(angleXComponent);
//        if(angleXComponent>1F){
//            angleXComponent=1F;
//        }
//        if(angleXComponent<-1F){
//            angleXComponent=-1F;
//        }
        //angleXComponent=angleXComponent/10;
        poseStack.mulPose(Axis.XP.rotation(angleYComponent));
        //poseStack.mulPose(quaternion);
        poseStack.mulPose(Axis.YP.rotation(angleXComponent));
        Lighting.setupForEntityInInventory();



//
//        poseStack.mulPose(Axis.XP.rotation(rx));
//        poseStack.mulPose(Axis.YP.rotation(ry));

        poseStack.scale(16.0F*scale, 16.0F*scale, 16.0F*scale);
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
        if (flag) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

//=======================================================================================

    public static void renderEntityInInventoryFollowsMouse(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, float pMouseX, float pMouseY, ItemEntity pEntity) {
        float f = (float)Math.atan((double)(pMouseX / 40.0F));
        float f1 = (float)Math.atan((double)(pMouseY / 40.0F));
        // Forge: Allow passing in direct angle components instead of mouse position
        renderEntityInInventoryFollowsAngle(pGuiGraphics, pX, pY, pScale, f, f1, pEntity);
    }

    public static void renderEntityInInventoryFollowsAngle(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, float angleXComponent, float angleYComponent, ItemEntity pEntity) {
        float f = angleXComponent;
        float f1 = angleYComponent;
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(f1 * 20.0F * ((float)Math.PI / 180F));
        quaternionf.mul(quaternionf1);
        //float f2 = pEntity.yBodyRot;
        float f3 = pEntity.getYRot();
        float f4 = pEntity.getXRot();
        //float f5 = pEntity.yHeadRotO;
        //float f6 = pEntity.yHeadRot;
        //pEntity.yBodyRot = 180.0F + f * 20.0F;
        //pEntity.setYRot(180.0F + f * 40.0F);
       // pEntity.setXRot(-f1 * 20.0F);
        //pEntity.setYRot(0F);
        //pEntity.setXRot(0F);
        //pEntity.yHeadRot = pEntity.getYRot();
        //pEntity.yHeadRotO = pEntity.getYRot();
        renderEntityInInventory(pGuiGraphics, pX, pY, pScale, quaternionf, quaternionf1, pEntity);
        //pEntity.yBodyRot = f2;
        //pEntity.setYRot(f3);
        //pEntity.setXRot(f4);
        //pEntity.yHeadRotO = f5;
        //pEntity.yHeadRot = f6;
    }

    public static void renderEntityInInventory(GuiGraphics pGuiGraphics, int pX, int pY, int pScale, Quaternionf pPose, @Nullable Quaternionf pCameraOrientation, ItemEntity pEntity) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((double)pX, (double)pY, 50.0D);
        pGuiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling((float)pScale, (float)pScale, (float)(-pScale)));
        pGuiGraphics.pose().mulPose(pPose);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (pCameraOrientation != null) {
            pCameraOrientation.conjugate();
            entityrenderdispatcher.overrideCameraOrientation(pCameraOrientation);
        }

        entityrenderdispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(pEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, pGuiGraphics.pose(), pGuiGraphics.bufferSource(), 15728880);
        });
        pGuiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        pGuiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }






}
