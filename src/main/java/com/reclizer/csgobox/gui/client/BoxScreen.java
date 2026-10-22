package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.utils.BlurHandler;
import com.reclizer.csgobox.utils.BoxViewport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.UUID;

/** Own HUD/effect lifetime, including Escape, death, disconnect and screen replacement. */
public abstract class BoxScreen extends Screen {
    protected static final int LAYOUT_WIDTH = BoxViewport.WIDTH;
    protected static final int LAYOUT_HEIGHT = BoxViewport.HEIGHT;
    private Boolean previousHideGui;
    private long backgroundStarted;
    private boolean continuingBlur;
    private final BoxScreen returnScreen;
    private int heartbeatTicks;
    private BoxViewport viewport = BoxViewport.fit(LAYOUT_WIDTH, LAYOUT_HEIGHT);

    protected BoxScreen(Component title) { this(title, null); }
    protected BoxScreen(Component title, BoxScreen returnScreen) { super(title); this.returnScreen = returnScreen; }
    protected final boolean isInspection() { return returnScreen != null; }
    protected UUID previewToken() { return returnScreen == null ? null : returnScreen.previewToken(); }

    private void heartbeat() {
        UUID token = previewToken();
        if (token != null && minecraft.getConnection() != null)
            com.reclizer.csgobox.packet.Networking.INSTANCE.sendToServer(new com.reclizer.csgobox.packet.PacketPreviewHeartbeat(token));
    }

    protected final void showNext(BoxScreen next) {
        next.backgroundStarted = backgroundStarted;
        continuingBlur = true;
        try { minecraft.setScreen(next); }
        finally {
            continuingBlur = false;
            // A Forge screen event may cancel or replace the requested transition.
            if (!(minecraft.screen instanceof BoxScreen)) BlurHandler.updateShader(true);
        }
    }

    @Override
    protected void init() {
        super.init();
        viewport = BoxViewport.fit(width, height);
        if (previousHideGui == null) {
            previousHideGui = minecraft.options.hideGui;
            if (backgroundStarted == 0) backgroundStarted = net.minecraft.Util.getMillis();
        }
        minecraft.options.hideGui = true;
        heartbeatTicks = 0;
        heartbeat();
    }

    @Override
    public void removed() {
        if (!continuingBlur) BlurHandler.updateShader(true);
        if (minecraft != null && previousHideGui != null) minecraft.options.hideGui = previousHideGui;
        previousHideGui = null;
        super.removed();
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level == null) {
            // A dead/disconnected player must leave the entire hierarchy, not reopen a parent.
            minecraft.setScreen(null);
            return;
        }
        if (++heartbeatTicks >= 200) { heartbeatTicks = 0; heartbeat(); }
    }

    @Override public void onClose() {
        if (returnScreen != null && minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null)
            showNext(returnScreen);
        else super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int localX = (int) Math.floor(viewport.localX(mouseX));
        int localY = (int) Math.floor(viewport.localY(mouseY));
        clipCanvas(graphics);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(viewport.left(), viewport.top(), 0);
            // Keep GUI depth independent of the window / GUI scale.
            graphics.pose().scale((float) viewport.scale(), (float) viewport.scale(), 1);
            renderContents(graphics, localX, localY, partialTick);
            super.render(graphics, localX, localY, partialTick);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
        renderHover(graphics, mouseX, mouseY);
    }

    protected abstract void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** Keep the logical transform, but allow an overlay to cover the window's letterbox margins. */
    protected final void renderWindowOverlay(GuiGraphics graphics, Consumer<BoxViewport> render) {
        graphics.flush();
        graphics.disableScissor();
        try {
            render.accept(viewport);
            graphics.flush();
        } finally { clipCanvas(graphics); }
    }

    private void clipCanvas(GuiGraphics graphics) {
        // GuiGraphics scissors use screen coordinates, independently of the pose stack.
        graphics.enableScissor((int) Math.floor(viewport.left()), (int) Math.floor(viewport.top()),
                (int) Math.ceil(viewport.screenX(LAYOUT_WIDTH)), (int) Math.ceil(viewport.screenY(LAYOUT_HEIGHT)));
    }

    protected void renderHover(GuiGraphics graphics, int mouseX, int mouseY) {}
    protected boolean canvasClicked(double x, double y, int button) { return false; }
    protected boolean canvasDragged(int button, double dx, double dy) { return false; }
    protected boolean canvasScrolled(double x, double y, double delta) { return false; }
    protected void canvasReleased(int button) {}

    @Override public final boolean mouseClicked(double x, double y, int button) {
        double localX = viewport.localX(x), localY = viewport.localY(y);
        return canvasClicked(localX, localY, button) || super.mouseClicked(localX, localY, button);
    }

    @Override public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        double localDx = viewport.localDelta(dx), localDy = viewport.localDelta(dy);
        return canvasDragged(button, localDx, localDy)
                || super.mouseDragged(viewport.localX(x), viewport.localY(y), button, localDx, localDy);
    }

    @Override public final boolean mouseReleased(double x, double y, int button) {
        canvasReleased(button);
        return super.mouseReleased(viewport.localX(x), viewport.localY(y), button);
    }

    @Override public final boolean mouseScrolled(double x, double y, double delta) {
        double localX = viewport.localX(x), localY = viewport.localY(y);
        return canvasScrolled(localX, localY, delta) || super.mouseScrolled(localX, localY, delta);
    }

    @Override public final void mouseMoved(double x, double y) {
        super.mouseMoved(viewport.localX(x), viewport.localY(y));
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        BlurHandler.updateShader(false);
        float fade = Math.min(1, (net.minecraft.Util.getMillis() - backgroundStarted) / 100F);
        int alpha = (int)Math.round(255 * com.reclizer.csgobox.config.ClientConfig.BACKGROUND_OPACITY.get() * fade);
        int shade = (int)(90 * fade);
        int color = (alpha << 24) | (shade << 16) | (shade << 8) | shade;
        graphics.fill(0, 0, width, height, color);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, graphics));
    }
}
