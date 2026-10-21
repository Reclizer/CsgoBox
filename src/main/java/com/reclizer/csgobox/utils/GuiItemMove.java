package com.reclizer.csgobox.utils;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Delegate item rendering and lighting to Minecraft instead of global renderer state. */
public final class GuiItemMove {
    private GuiItemMove() {}

    public static float renderRotAngleY(double delta, float angle) {
        return Mth.clamp(angle + (float) Math.atan(delta / 40), -1.5F, 1.5F);
    }

    public static float renderRotAngleX(double delta, float angle) {
        return Mth.clamp(angle + (float) Math.atan(delta / 40), -3, 3);
    }

    public static void renderItemInInventoryFollowsMouse(GuiGraphics graphics, int x, int y,
            float angleX, float angleY, ItemStack item, LivingEntity player, float scale) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + 8 * scale, y + 8 * scale, 0);
            graphics.pose().mulPose(Axis.XP.rotation(angleY));
            graphics.pose().mulPose(Axis.YP.rotation(angleX));
            graphics.pose().scale(scale, scale, scale);
            graphics.renderItem(item, -8, -8);
            graphics.flush();
        } finally { graphics.pose().popPose(); }
    }
}
