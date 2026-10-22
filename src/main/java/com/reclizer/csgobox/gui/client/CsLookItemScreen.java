package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.utils.PreviewRotation;
import com.reclizer.csgobox.utils.HoverCache;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Keep the original reveal composition while displaying only the server-confirmed reward. */
public class CsLookItemScreen extends BoxScreen {
    static final int MODEL_SIZE = LAYOUT_WIDTH*26/100;
    static final float MODEL_CENTER_X = LAYOUT_WIDTH*37/100 + MODEL_SIZE/2F;
    static final float MODEL_CENTER_Y = LAYOUT_HEIGHT*30/100 + MODEL_SIZE/2F;
    private final ItemStack reward;
    private final int grade;
    private final PreviewRotation rotation = new PreviewRotation();
    private boolean playedSound;
    private final HoverCache<List<Component>> tooltip = new HoverCache<>();

    public CsLookItemScreen(ItemStack reward, int grade) {
        this(reward, grade, null);
    }

    public CsLookItemScreen(ItemStack reward, int grade, BoxScreen returnScreen) {
        super(com.reclizer.csgobox.compat.TaczCompat.rewardName(reward), returnScreen);
        this.reward = reward.copy();
        this.grade = grade;
    }

    @Override protected void init() {
        super.init();
        rotation.release(Util.getMillis());
        tooltip.clear();
        if (!isInspection() && !playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_FINSH.get(), 1, 1);
            playedSound = true;
        }
        addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 72, "back_box",
                Component.translatable(isInspection() ? "gui.csgobox.inspect.back" : "gui.csgobox.csgo_box.back_box"), button -> onClose()));
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
        ClassicBoxUi.centeredText(g, font, title,
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.05F, 1.8F, LAYOUT_WIDTH*0.9F, 0xFFFFFFFF);
        ClassicBoxUi.centeredText(g, font, Component.translatable("gui.csgobox.csgo_box.grade"+grade),
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.11F, 1, LAYOUT_WIDTH*0.9F, 0xFFFFFFFF);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, MODEL_CENTER_X-MODEL_SIZE/2F, MODEL_CENTER_Y-MODEL_SIZE/2F,
                    rotation.yaw(), rotation.pitch(), reward, minecraft.player, MODEL_SIZE/16F);
        if (isInspection()) ClassicBoxUi.centeredText(g, font, Component.translatable("gui.csgobox.inspect.drag_hint"),
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.86F, 0.65F, LAYOUT_WIDTH*0.8F, 0xFFD3D3D3);
    }

    @Override protected void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        // Shift provides the original item's complete Forge tooltip without covering the model by default.
        if (isInspection() && hasShiftDown()) {
            int context = (hasControlDown()?1:0) | (hasAltDown()?2:0) | (minecraft.options.advancedItemTooltips?4:0);
            var lines = tooltip.get(reward, context, Util.getMillis(), () -> List.copyOf(getTooltipFromItem(minecraft, reward)));
            g.renderTooltip(font, lines, reward.getTooltipImage(), reward, mouseX, mouseY);
        } else tooltip.clear();
    }
}
