package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.ReelPresentation;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Original CSGO presentation, with rendering isolated from inventory and network operations. */
final class ClassicBoxUi {
    private static final ResourceLocation GOLD = new ResourceLocation("csgobox:textures/screens/gold_item.png");
    private ClassicBoxUi() {}

    static void text(GuiGraphics g, Font font, Component text, float x, float y, float scale, int color) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 300);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, text, 0, 0, color, false);
        } finally { g.pose().popPose(); }
    }

    /** Centre the complete styled text after scaling, keeping long names inside the heading area. */
    static void centeredText(GuiGraphics g, Font font, Component text, float centerX, float y,
                             float scale, float maxWidth, int color) {
        int textWidth = font.width(text);
        float fittedScale = Math.min(scale, maxWidth / Math.max(1, textWidth));
        text(g, font, text, centerX - textWidth * fittedScale / 2, y, fittedScale, color);
    }

    static void item(GuiGraphics g, ItemStack stack, float x, float y, float scale) {
        if (stack.isEmpty()) return;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1);
            g.renderItem(stack, 0, 0);
            // renderItem already flushes before restoring item lighting.
        } finally { g.pose().popPose(); }
    }

    static void previewCard(GuiGraphics g, ItemStack stack, int grade, int x, int y, int screenWidth, int screenHeight,
                            CardItemLayout layout) {
        rewardCard(g, stack, grade, x, y, Math.max(5, screenWidth * 8 / 100),
                Math.max(5, screenHeight * 11 / 100), layout, true);
    }

    static void rewardCard(GuiGraphics g, ItemStack stack, int grade, int x, int y, int w, int h,
                           CardItemLayout layout, boolean mysteryGold) {
        g.fillGradient(x, y, x+w, y+h, grade == 5 ? 0xFF554A33 : 0xFF666970, grade == 5 ? 0xFF827044 : 0xFF85878C);
        g.fill(x, y, x+2, y+h, ColorTools.colorItems(grade));
        if (grade == 5 && mysteryGold) gold(g, x+2, y+2, w-4, h-4);
        else {
            float size = layout.size(w, h);
            item(g, stack, x+(w-size)/2, y+(h-size)/2, size/16);
        }
    }

    static void hoverFrame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x+w, y+1, color); g.fill(x, y+h-1, x+w, y+h, color);
        g.fill(x, y, x+1, y+h, color); g.fill(x+w-1, y, x+w, y+h, color);
    }

    static void reelCard(GuiGraphics g, ItemStack stack, int grade, float x, float y,
                         int screenWidth, int screenHeight, float reveal, CardItemLayout layout) {
        int w = Math.max(5, screenWidth * 18 / 100), h = Math.max(5, screenHeight * 25 / 100);
        float centerX = x + w/2F, centerY = y + h/2F;
        float depth = ReelPresentation.depth(centerX / screenWidth);
        if (reveal > 0) {
            centerX = ReelPresentation.mix(centerX, CsLookItemScreen.MODEL_CENTER_X, reveal);
            centerY = ReelPresentation.mix(centerY, CsLookItemScreen.MODEL_CENTER_Y, reveal);
        }
        g.pose().pushPose();
        try {
            g.pose().translate(centerX, centerY, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(Math.signum(centerX-screenWidth/2F) * 2 * depth));
            g.pose().scale(1 - 0.24F * depth, 1 - 0.17F * depth, 1);
            g.pose().translate(-w/2F, -h/2F, 0);
            if (grade == 5) {
                g.fillGradient(0, 0, w, h, 0xFF554A33, 0xFF827044);
                gold(g, 2, 2, w-4, h-4);
            } else {
                g.fillGradient(0, 0, w, h, 0xFF77787C, 0xFF8E8E91);
                int tint = ColorTools.colorItems(grade) & 0xFFFFFF;
                g.fillGradient(0, h*3/5, w, h, tint, 0xB0000000 | tint);
                float size = layout.size(w, h);
                item(g, stack, (w-size)/2, (h-size)/2 - 1, size/16);
            }
            g.fill(0, h, w, h+2, ColorTools.colorItems(grade));
            g.flush();
        } finally { g.pose().popPose(); }
    }

    static void gold(GuiGraphics g, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        g.blit(GOLD, x, y, 0, 0, w, h, w, h);
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
