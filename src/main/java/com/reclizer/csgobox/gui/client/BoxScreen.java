package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.utils.BlurHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Own HUD/effect lifetime, including Escape, death, disconnect and screen replacement. */
public abstract class BoxScreen extends Screen {
    private Boolean previousHideGui;

    protected BoxScreen(Component title) { super(title); }

    @Override
    protected void init() {
        super.init();
        if (previousHideGui == null) previousHideGui = minecraft.options.hideGui;
        minecraft.options.hideGui = true;
    }

    @Override
    public void removed() {
        BlurHandler.updateShader(true);
        if (minecraft != null && previousHideGui != null) minecraft.options.hideGui = previousHideGui;
        previousHideGui = null;
        super.removed();
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive()) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        BlurHandler.updateShader(false);
        graphics.fillGradient(0, 0, width, height, 0xDD151A22, 0xEE080C12);
    }
}
