package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.GuiItemMove;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CsLookItemScreen extends BoxScreen {
    private final ItemStack reward;
    private final int grade;
    private float rotationX;
    private float rotationY;
    private boolean playedSound;

    public CsLookItemScreen(ItemStack reward, int grade) {
        super(reward.getHoverName());
        this.reward = reward.copy();
        this.grade = grade;
    }

    @Override
    protected void init() {
        super.init();
        if (!playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_FINSH.get(), 1, 1);
            playedSound = true;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose())
                .bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0) {
            rotationX = GuiItemMove.renderRotAngleX(dx, rotationX);
            rotationY = GuiItemMove.renderRotAngleY(dy, rotationY);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.csgobox.csgo_box.grade" + grade),
                width / 2, 44, ColorTools.colorItems(grade));
        float size = Math.min(width * 0.25F, height * 0.35F);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(graphics, width / 2 - (int) size / 2,
                    height / 2 - (int) size / 2, rotationX, rotationY, reward, minecraft.player, size / 16F);
        if (reward.getCount() > 1) graphics.drawCenteredString(font, "× " + reward.getCount(), width / 2, height - 60, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
