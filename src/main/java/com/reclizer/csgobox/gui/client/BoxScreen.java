package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.utils.BlurHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Own HUD/effect lifetime, including Escape, death, disconnect and screen replacement. */
public abstract class BoxScreen extends Screen {
    private Boolean previousHideGui;
    private long backgroundStarted;

    protected BoxScreen(Component title) { super(title); }

    @Override
    protected void init() {
        super.init();
        if (previousHideGui == null) {
            previousHideGui = minecraft.options.hideGui;
            backgroundStarted = net.minecraft.Util.getMillis();
        }
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
        float fade = Math.min(1, (net.minecraft.Util.getMillis() - backgroundStarted) / 100F);
        int alpha = (int)(128 * fade), shade = (int)(90 * fade);
        int color = (alpha << 24) | (shade << 16) | (shade << 8) | shade;
        graphics.fill(0, 0, width, height, color);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, graphics));
    }
}
