package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.utils.PreviewRotation;
import com.reclizer.csgobox.utils.HoverCache;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import java.util.UUID;

/** Original preview layout; only the server may select and grant the reward. */
public class CsboxScreen extends BoxScreen {
    private static final int PAGE_SIZE = 20;
    // Leave room for the rotated box above the reward list; retain its original centre.
    private static final int MODEL_SIZE = LAYOUT_WIDTH * 21 / 100;
    private static final int MODEL_X = (LAYOUT_WIDTH - MODEL_SIZE) / 2;
    private static final int MODEL_Y = LAYOUT_HEIGHT * 35 / 100 - MODEL_SIZE / 2;
    private final PacketBoxPreview preview;
    private final Component unlockLabel;
    private final Component requiredKeyLabel;
    private boolean requested;
    private boolean playedSound;
    private Button openButton;
    private final PreviewRotation rotation = new PreviewRotation();
    private DisplayReward hovered;
    private final HoverCache<List<Component>> tooltip = new HoverCache<>();
    private List<List<FormattedCharSequence>> rewardNames = List.of();
    private final List<CardItemLayout> rewardLayouts;
    private int availableKeys;

    public CsboxScreen(PacketBoxPreview preview) {
        super(preview.box().getHoverName());
        this.preview = preview;
        this.rewardLayouts = preview.rewards().stream().map(entry -> CardItemLayout.of(entry.stack())).toList();
        this.unlockLabel = Component.translatable("gui.csgobox.csgo_box.label_box")
                .append(" ").append(preview.box().getHoverName());
        this.requiredKeyLabel = Component.translatable("gui.csgobox.csgo_box.requires_key", preview.key().getHoverName());
    }
    public UUID token() { return preview.token(); }
    @Override protected UUID previewToken() { return requested ? null : token(); }
    public List<DisplayReward> rewards() { return preview.rewards(); }

    @Override protected void init() {
        super.init();
        rotation.release(Util.getMillis());
        tooltip.clear();
        rewardNames = rewards().stream().map(entry -> {
            Component name = entry.grade() == 5 ? Component.translatable("gui.csgobox.csgo_box.label_gold")
                    : entry.stack().getHoverName();
            return font.split(name, (int)(LAYOUT_WIDTH * 0.08F / 0.6F)).stream().limit(2).toList();
        }).toList();
        availableKeys = keyCount();
        if (!playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_OPEN.get(), 1, 1);
            playedSound = true;
        }
        openButton = addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 67, "open_box",
                Component.translatable("gui.csgobox.csgo_box.open_box"), button -> {
                    if (!requested && (preview.key().isEmpty() || keyCount() > 0)) {
                        requested = true;
                        button.active = false;
                        Networking.INSTANCE.sendToServer(new PacketCsgoProgress(token()));
                    }
                }));
        openButton.active = !requested && (preview.key().isEmpty() || keyCount() > 0);
        addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 72, "back_box",
                Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose()));
    }

    private int keyCount() {
        if (minecraft.player == null) return 0;
        return minecraft.player.getInventory().items.stream()
                .filter(stack -> com.reclizer.csgobox.utils.BoxKeys.matches(preview.key(), stack)
                        && stack != minecraft.player.getMainHandItem())
                .mapToInt(ItemStack::getCount).sum();
    }

    @Override public void tick() {
        super.tick();
        availableKeys = keyCount();
        if (openButton != null) openButton.active = !requested && (preview.key().isEmpty() || availableKeys > 0);
    }

    @Override protected boolean canvasDragged(int button, double dx, double dy) {
        return button == 0 && rotation.drag(dx, dy, Util.getMillis());
    }

    @Override protected void canvasReleased(int button) {
        if (button == 0) rotation.release(Util.getMillis());
    }

    @Override protected boolean canvasScrolled(double x, double y, double delta) {
        return false;
    }

    @Override protected boolean canvasClicked(double x, double y, int button) {
        if (button == 0 && !requested) {
            DisplayReward entry = rewardAt(x, y);
            if (entry != null) {
                rotation.release(Util.getMillis()); tooltip.clear();
                if (entry.grade() == 5) showNext(new LegendaryRewardsScreen(this, preview.legendaryRewards(), preview.box().getHoverName()));
                else showNext(new CsLookItemScreen(entry.stack(), entry.grade(), this));
                return true;
            }
        }
        if (button == 0 && x >= MODEL_X && x <= MODEL_X + MODEL_SIZE
                && y >= MODEL_Y && y <= MODEL_Y + MODEL_SIZE) {
            rotation.grab(Util.getMillis());
            return true;
        }
        return false;
    }

    private DisplayReward rewardAt(double mouseX, double mouseY) {
        if (rewards().size() > PAGE_SIZE && rewards().get(PAGE_SIZE).grade() == 5
                && mouseX >= 444 && mouseX < 466 && mouseY >= 122 && mouseY < 142) return rewards().get(PAGE_SIZE);
        for (int n = 0; n < Math.min(rewards().size(), PAGE_SIZE); n++) {
            int x = LAYOUT_WIDTH*4/100 + n%10*LAYOUT_WIDTH*9/100;
            int y = LAYOUT_HEIGHT*(n < 10 ? 55 : 73)/100;
            if (mouseX >= x && mouseX < x+LAYOUT_WIDTH*8/100 && mouseY >= y && mouseY < y+LAYOUT_HEIGHT*17/100)
                return rewards().get(n);
        }
        return null;
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!minecraft.isWindowActive()) rotation.release(Util.getMillis());
        rotation.update(Util.getMillis());
        g.fill(LAYOUT_WIDTH*3/100, LAYOUT_HEIGHT*53/100, LAYOUT_WIDTH*97/100, LAYOUT_HEIGHT*53/100+1, 0xFFD3D3D3);
        g.fill(LAYOUT_WIDTH*25/100, LAYOUT_HEIGHT*92/100, LAYOUT_WIDTH*75/100, LAYOUT_HEIGHT*92/100+1, 0xFFD3D3D3);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, MODEL_X, MODEL_Y,
                    rotation.yaw(), rotation.pitch(), preview.box(), minecraft.player, MODEL_SIZE/16F,
                    BoxAppearanceModels.appearance(preview.box()) != com.reclizer.csgobox.config.BoxAppearance.CLASSIC);
        Style bold = Style.EMPTY.withBold(true);
        ClassicBoxUi.centeredText(g, font, Component.translatable("gui.csgobox.csgo_box.title").withStyle(bold),
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.059F, 2, LAYOUT_WIDTH*0.9F, 0xFFD3D3D3);
        ClassicBoxUi.centeredText(g, font, unlockLabel,
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.13F, 0.8F, LAYOUT_WIDTH*0.9F, 0xFFD3D3D3);
        label(g, Component.translatable("gui.csgobox.csgo_box.label_items").withStyle(bold), 3, 50.3F, 0.8F);

        hovered = rotation.isDragging() || requested ? null : rewardAt(mouseX, mouseY);
        for (int n = 0; n < Math.min(rewards().size(), PAGE_SIZE); n++) {
            int index = n%PAGE_SIZE, column = index%10, row = index/10;
            int x = LAYOUT_WIDTH*4/100 + column*LAYOUT_WIDTH*9/100, y = LAYOUT_HEIGHT*(row == 0 ? 55 : 73)/100;
            DisplayReward entry = rewards().get(n);
            ClassicBoxUi.previewCard(g, entry.stack(), entry.grade(), x, y, LAYOUT_WIDTH, LAYOUT_HEIGHT, rewardLayouts.get(n));
            if (hovered == entry) ClassicBoxUi.hoverFrame(g, x, y, LAYOUT_WIDTH*8/100, LAYOUT_HEIGHT*11/100, 0xFFE9D49E);
            // Long names wrap inside their original card column instead of spilling into adjacent cards.
            int line = 0;
            for (var part : rewardNames.get(n)) {
                g.pose().pushPose();
                try {
                    g.pose().translate(LAYOUT_WIDTH*0.04F + column*LAYOUT_WIDTH*0.09F, LAYOUT_HEIGHT*(row == 0 ? 0.67F : 0.85F) + line*6F, 300);
                    g.pose().scale(0.6F, 0.6F, 1);
                    g.drawString(font, part, 0, 0, 0xFFD3D3D3, false);
                } finally { g.pose().popPose(); }
                line++;
            }
        }
        if (rewards().size() > PAGE_SIZE && rewards().get(PAGE_SIZE).grade() == 5) {
            // Twenty regular cards retain their original grid; the extra gold badge uses the former page controls.
            ClassicBoxUi.gold(g, 447, 124, 17, 17);
            if (hovered == rewards().get(PAGE_SIZE)) ClassicBoxUi.hoverFrame(g, 444, 122, 22, 20, 0xFFE9D49E);
        }
        if (!preview.key().isEmpty()) {
            ClassicBoxUi.item(g, preview.key(), LAYOUT_WIDTH*0.25F, LAYOUT_HEIGHT*0.93F, 1);
            int count = availableKeys;
            if (count > 0) label(g, Component.literal(" × " + count), 28, 94, 0.8F);
            else {
                // Lay out the whole translated sentence, leaving space for the icon and button.
                float x = LAYOUT_WIDTH * 0.25F + 16 + 2;
                float maxWidth = openButton.getX() - 4 - x;
                float scale = Math.min(0.8F, maxWidth / Math.max(1, font.width(requiredKeyLabel)));
                ClassicBoxUi.text(g, font, requiredKeyLabel, x, LAYOUT_HEIGHT * 0.94F, scale, 0xFFD3D3D3);
            }
        } else label(g, Component.translatable("gui.csgobox.csgo_box.no_key"), 25, 94, 0.8F);
    }

    // Vanilla tooltip positioning needs the real screen coordinates, outside the canvas transform.
    @Override protected void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        if (hovered != null) {
            if (hovered.grade() == 5) g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.csgobox.csgo_box.label_gold"),
                    Component.translatable("gui.csgobox.inspect.legendary_hint").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
            else {
                int context = (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0)
                        | (hasAltDown() ? 4 : 0) | (minecraft.options.advancedItemTooltips ? 8 : 0);
                ItemStack stack = hovered.stack();
                List<Component> lines = tooltip.get(hovered, context, Util.getMillis(),
                        () -> {
                            var description = new java.util.ArrayList<>(getTooltipFromItem(minecraft, stack));
                            description.add(Component.translatable("gui.csgobox.inspect.click_hint").withStyle(net.minecraft.ChatFormatting.GRAY));
                            return List.copyOf(description);
                        });
                // Keep Forge's item-aware layout, tooltip images and render hooks (including custom fonts).
                // Only expensive item-description generation is cached, not the pointer position.
                g.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
            }
        } else tooltip.clear();
    }

    private void label(GuiGraphics g, Component text, float x, float y, float scale) {
        ClassicBoxUi.text(g, font, text, LAYOUT_WIDTH*x/100, LAYOUT_HEIGHT*y/100, scale, 0xFFD3D3D3);
    }
}
