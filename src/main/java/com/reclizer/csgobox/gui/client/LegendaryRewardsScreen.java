package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.compat.TaczCompat;
import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.utils.HoverCache;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** All legendary rewards from this preview, with real icons and local read-only inspection. */
public final class LegendaryRewardsScreen extends BoxScreen {
    private static final int PAGE_SIZE = 32, COLUMNS = 8, LEFT = 28, TOP = 48, STEP_X = 54, STEP_Y = 45;
    private final List<DisplayReward> rewards;
    private final List<CardItemLayout> layouts;
    private final Component boxName;
    private final HoverCache<List<Component>> tooltip = new HoverCache<>();
    private List<List<FormattedCharSequence>> names = List.of();
    private int page;
    private DisplayReward hovered;
    private Button previous, next;

    public LegendaryRewardsScreen(BoxScreen parent, List<DisplayReward> rewards, Component boxName) {
        super(Component.translatable("gui.csgobox.inspect.legendary_title"), parent);
        this.rewards = List.copyOf(rewards);
        this.boxName = boxName.copy();
        layouts = rewards.stream().map(entry -> CardItemLayout.of(entry.stack())).toList();
    }

    @Override protected void init() {
        super.init(); tooltip.clear();
        names = rewards.stream().map(entry -> font.split(TaczCompat.rewardName(entry.stack()), 83).stream().limit(2).toList()).toList();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1)).bounds(192, 232, 20, 15).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1)).bounds(268, 232, 20, 15).build());
        addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 72, "back_box",
                Component.translatable("gui.csgobox.inspect.back"), b -> onClose()));
        updateButtons();
    }
    private int pages() { return Math.max(1, (rewards.size()+PAGE_SIZE-1)/PAGE_SIZE); }
    private void updateButtons() {
        previous.visible = next.visible = pages() > 1;
        previous.active = page > 0; next.active = page+1 < pages();
    }
    private void changePage(int delta) {
        page = Math.max(0, Math.min(pages()-1, page+delta));
        hovered = null; tooltip.clear(); updateButtons();
    }
    private int entryAt(double x, double y) {
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page*PAGE_SIZE+i;
            if (index >= rewards.size()) break;
            int cx = LEFT+i%COLUMNS*STEP_X, cy = TOP+i/COLUMNS*STEP_Y;
            if (x >= cx && x < cx+46 && y >= cy && y < cy+43) return index;
        }
        return -1;
    }
    @Override protected boolean canvasClicked(double x, double y, int button) {
        int index = entryAt(x,y);
        if (button != 0 || index < 0) return false;
        var reward = rewards.get(index); tooltip.clear();
        showNext(new CsLookItemScreen(reward.stack(), reward.grade(), this));
        return true;
    }
    @Override protected boolean canvasScrolled(double x, double y, double delta) {
        if (delta == 0 || pages() <= 1) return false;
        changePage(delta < 0 ? 1 : -1); return true;
    }
    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ClassicBoxUi.centeredText(g, font, title, 240, 13, 1.5F, 440, 0xFFF3D280);
        ClassicBoxUi.centeredText(g, font, boxName, 240, 31, .7F, 440, 0xFFD3D3D3);
        int at = entryAt(mouseX,mouseY);
        hovered = at < 0 ? null : rewards.get(at);
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page*PAGE_SIZE+i;
            if (index >= rewards.size()) break;
            int x = LEFT+i%COLUMNS*STEP_X, y = TOP+i/COLUMNS*STEP_Y;
            var entry = rewards.get(index);
            ClassicBoxUi.rewardCard(g, entry.stack(), 5, x, y, 46, 29, layouts.get(index), false);
            if (hovered == entry) ClassicBoxUi.hoverFrame(g, x, y, 46, 29, 0xFFFFE7A6);
            int line = 0;
            for (var text : names.get(index)) {
                g.pose().pushPose();
                g.pose().translate(x, y+31+line*6, 300); g.pose().scale(.55F,.55F,1);
                g.drawString(font, text, 0, 0, 0xFFF3D280, false);
                g.pose().popPose(); line++;
            }
        }
        if (rewards.isEmpty()) ClassicBoxUi.centeredText(g, font, Component.translatable("gui.csgobox.inspect.empty"), 240, 132, .9F, 440, 0xFFD3D3D3);
        if (pages() > 1) ClassicBoxUi.centeredText(g, font, Component.literal((page+1)+" / "+pages()), 240, 237, .7F, 50, 0xFFD3D3D3);
        g.fill(120, 248, 360, 249, 0xFFD3D3D3);
        ClassicBoxUi.text(g, font, Component.translatable("gui.csgobox.inspect.legendary_count", rewards.size()), 120, 255, .65F, 0xFFF3D280);
    }
    @Override protected void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        if (hovered == null) { tooltip.clear(); return; }
        int context = (hasShiftDown()?1:0) | (hasControlDown()?2:0) | (hasAltDown()?4:0)
                | (minecraft.options.advancedItemTooltips?8:0);
        var entry = hovered;
        List<Component> lines = tooltip.get(entry, context, Util.getMillis(), () -> {
            var description = new ArrayList<>(getTooltipFromItem(minecraft, entry.stack()));
            description.add(Component.translatable("gui.csgobox.inspect.click_hint").withStyle(ChatFormatting.GRAY));
            return List.copyOf(description);
        });
        g.renderTooltip(font, lines, entry.stack().getTooltipImage(), entry.stack(), mouseX, mouseY);
    }
}
