package com.reclizer.csgobox.utils;

import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

/** Rotate the model around its centre, without rotating the GUI depth translation. */
public final class GuiItemMove {
    private GuiItemMove() {}

    public static void renderItemInInventoryFollowsMouse(GuiGraphics graphics, int x, int y,
            float angleX, float angleY, ItemStack item, LivingEntity player, float scale) {
        if (item.isEmpty()) return;
        var renderer = Minecraft.getInstance().getItemRenderer();
        var model = renderer.getModel(item, player.level(), player, 0);
        graphics.flush();
        graphics.pose().pushPose();
        try {
            // GuiGraphics.renderItem adds Z=150 internally. Calling it after rotating/scaling
            // would rotate that offset too, throwing the preview off-screen as soon as it turns.
            graphics.pose().translate(x + 8 * scale, y + 8 * scale, 150);
            graphics.pose().mulPose(Axis.XP.rotation(angleY));
            graphics.pose().mulPose(Axis.YP.rotation(angleX));
            graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));
            graphics.pose().scale(16 * scale, 16 * scale, 16 * scale);
            if (!model.usesBlockLight()) Lighting.setupForFlatItems();
            // TaCZ intentionally uses a flat slot texture in GUI context. FIXED uses its
            // gun-pack positioning, attachments and 3D model without first-person arms.
            var display = com.reclizer.csgobox.config.ClientConfig.TACZ_3D_PREVIEW.get()
                    && com.reclizer.csgobox.compat.TaczCompat.isGun(item) ? ItemDisplayContext.FIXED : ItemDisplayContext.GUI;
            renderer.render(item, display, false, graphics.pose(), graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }
}
