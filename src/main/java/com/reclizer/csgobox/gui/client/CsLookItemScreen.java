package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.GuiItemMove;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Keep the original reveal composition while displaying only the server-confirmed reward. */
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

    @Override protected void init() {
        super.init();
        if (!playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_FINSH.get(), 1, 1);
            playedSound = true;
        }
        addRenderableWidget(ClassicBoxUi.action(font, width, height, 72, "back_box",
                Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose()));
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        int size = width*26/100;
        if (button == 0 && x >= width*0.37F && x <= width*0.37F+size
                && y >= height*0.30F && y <= height*0.30F+size) {
            rotationX = GuiItemMove.renderRotAngleX(dx, rotationX);
            rotationY = GuiItemMove.renderRotAngleY(dy, rotationY);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(width*25/100, height*92/100, width*75/100, height*92/100+1, 0xFFD3D3D3);
        g.fill(width*37/100, height*16/100, width*63/100, height*16/100+2, ColorTools.colorItems(grade));
        ClassicBoxUi.text(g, font, title, width*0.45F, height*0.05F, 1.8F, 0xFFFFFFFF);
        ClassicBoxUi.text(g, font, Component.translatable("gui.csgobox.csgo_box.grade"+grade),
                width*0.45F, height*0.11F, 1, 0xFFFFFFFF);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, width*37/100, height*30/100,
                    rotationX, rotationY, reward, minecraft.player, (width*26/100)/16F);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
