package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.utils.ColorTools;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Original CSGO presentation, with rendering isolated from inventory and network operations. */
final class ClassicBoxUi {
    private static final ResourceLocation GOLD = new ResourceLocation("csgobox:textures/screens/gold_item.png");
    private static final ResourceLocation REEL_MASK = new ResourceLocation("csgobox:textures/screens/csgo_background.png");
    private ClassicBoxUi() {}

    static void text(GuiGraphics g, Font font, Component text, float x, float y, float scale, int color) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 300);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, text, 0, 0, color, false);
        } finally { g.pose().popPose(); }
    }

    static void item(GuiGraphics g, ItemStack stack, float x, float y, float scale) {
        if (stack.isEmpty()) return;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1);
            g.renderItem(stack, 0, 0);
            g.flush();
        } finally { g.pose().popPose(); }
    }

    static void previewCard(GuiGraphics g, ItemStack stack, int grade, int x, int y, int screenWidth, int screenHeight) {
        int w = Math.max(5, screenWidth * 8 / 100), h = Math.max(5, screenHeight * 11 / 100);
        g.fillGradient(x, y, x+w, y+h, grade == 5 ? 0xFF533C00 : 0xFF696969, grade == 5 ? 0xFFB69008 : 0xFFD3D3D3);
        g.fill(x, y, x+2, y+h, ColorTools.colorItems(grade));
        if (grade == 5) gold(g, x+2, y+2, w-4, h-4);
        else item(g, stack, x+w*20/100, y+h*10/100, w*0.6F/16);
    }

    static void reelCard(GuiGraphics g, ItemStack stack, int grade, float x, float y, int screenWidth, int screenHeight) {
        int w = Math.max(5, screenWidth * 18 / 100), h = Math.max(5, screenHeight * 25 / 100);
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            if (grade == 5) {
                g.fillGradient(0, 0, w, h, 0xFF533C00, 0xFFB69008);
                gold(g, 2, 2, w-4, h-4);
            } else {
                g.fillGradient(0, 0, w, h, 0xFF696969, 0xFFA9A9A9);
                g.fillGradient(0, h*2/3, w, h, 0x00808080, ColorTools.deepColor(ColorTools.colorItems(grade)));
                item(g, stack, w*0.2F, h*0.1F, w*0.6F/16);
            }
            g.fill(0, h, w, h+2, ColorTools.colorItems(grade));
            g.flush();
        } finally { g.pose().popPose(); }
    }

    private static void gold(GuiGraphics g, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        g.blit(GOLD, x, y, 0, 0, w, h, w, h);
    }

    static void reelMask(GuiGraphics g, int width, int height) {
        g.flush();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            RenderSystem.enableBlend();
            g.blit(REEL_MASK, 0, 0, 0, 0, width, height, width, height);
        } finally { g.pose().popPose(); }
    }

    /** Keep the original green/grey textures and percentage-based hit areas. */
    static Button action(Font font, int screenWidth, int screenHeight, int percentX,
                         String texture, Component label, Button.OnPress action) {
        return new Button(screenWidth*percentX/100, screenHeight*94/100,
                Math.max(1, screenWidth*4/100), Math.max(1, screenHeight*5/100),
                label, action, narration -> narration.get()) {
            private final ResourceLocation image = new ResourceLocation("csgobox:textures/screens/atlas/" + texture + ".png");
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                RenderSystem.enableBlend();
                g.blit(image, getX(), getY(), width, height, 0, isHoveredOrFocused() ? 64 : 0, 82, 64, 82, 128);
                text(g, font, getMessage(), screenWidth*(percentX+0.5F)/100F, screenHeight*0.95F,
                        0.8F, active ? 0xFFD3D3D3 : 0xFF939393);
            }
        };
    }
}
