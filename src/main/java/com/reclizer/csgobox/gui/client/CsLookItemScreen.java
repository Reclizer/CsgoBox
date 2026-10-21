package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.utils.PreviewRotation;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Keep the original reveal composition while displaying only the server-confirmed reward. */
public class CsLookItemScreen extends BoxScreen {
    private final ItemStack reward;
    private final int grade;
    private final PreviewRotation rotation = new PreviewRotation();
    private boolean playedSound;

    public CsLookItemScreen(ItemStack reward, int grade) {
        super(reward.getHoverName());
        this.reward = reward.copy();
        this.grade = grade;
    }

    @Override protected void init() {
        super.init();
        rotation.release(Util.getMillis());
        if (!playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_FINSH.get(), 1, 1);
            playedSound = true;
        }
        addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 72, "back_box",
                Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose()));
    }

    @Override protected boolean canvasClicked(double x, double y, int button) {
        if (button == 0 && x >= LAYOUT_WIDTH*0.37 && x <= LAYOUT_WIDTH*0.63
                && y >= LAYOUT_HEIGHT*0.30 && y <= LAYOUT_HEIGHT*0.30 + LAYOUT_WIDTH*0.26) {
            rotation.grab(Util.getMillis());
            return true;
        }
        return false;
    }

    @Override protected boolean canvasDragged(int button, double dx, double dy) {
        return button == 0 && rotation.drag(dx, dy, Util.getMillis());
    }

    @Override protected void canvasReleased(int button) {
        if (button == 0) rotation.release(Util.getMillis());
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!minecraft.isWindowActive()) rotation.release(Util.getMillis());
        rotation.update(Util.getMillis());
        g.fill(LAYOUT_WIDTH*25/100, LAYOUT_HEIGHT*92/100, LAYOUT_WIDTH*75/100, LAYOUT_HEIGHT*92/100+1, 0xFFD3D3D3);
        g.fill(LAYOUT_WIDTH*37/100, LAYOUT_HEIGHT*16/100, LAYOUT_WIDTH*63/100, LAYOUT_HEIGHT*16/100+2, ColorTools.colorItems(grade));
        ClassicBoxUi.text(g, font, title, LAYOUT_WIDTH*0.45F, LAYOUT_HEIGHT*0.05F, 1.8F, 0xFFFFFFFF);
        ClassicBoxUi.text(g, font, Component.translatable("gui.csgobox.csgo_box.grade"+grade),
                LAYOUT_WIDTH*0.45F, LAYOUT_HEIGHT*0.11F, 1, 0xFFFFFFFF);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, LAYOUT_WIDTH*37/100, LAYOUT_HEIGHT*30/100,
                    rotation.yaw(), rotation.pitch(), reward, minecraft.player, (LAYOUT_WIDTH*26/100)/16F);
    }
}
