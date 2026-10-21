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
    private int page;
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
                    if (!requested && keyCount() > 0) {
                        requested = true;
                        button.active = false;
                        Networking.INSTANCE.sendToServer(new PacketCsgoProgress(token()));
                    }
                }));
        openButton.active = !requested && keyCount() > 0;
        addRenderableWidget(ClassicBoxUi.action(font, LAYOUT_WIDTH, LAYOUT_HEIGHT, 72, "back_box",
                Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose()));
    }

    private int keyCount() {
        if (minecraft.player == null) return 0;
        return minecraft.player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && stack.is(preview.key().getItem()))
                .mapToInt(ItemStack::getCount).sum();
    }

    @Override public void tick() {
        super.tick();
        availableKeys = keyCount();
        if (openButton != null) openButton.active = !requested && availableKeys > 0;
    }

    @Override protected boolean canvasDragged(int button, double dx, double dy) {
        return button == 0 && rotation.drag(dx, dy, Util.getMillis());
    }

    @Override protected void canvasReleased(int button) {
        if (button == 0) rotation.release(Util.getMillis());
    }

    @Override protected boolean canvasScrolled(double x, double y, double delta) {
        if (rewards().size() > PAGE_SIZE && y >= LAYOUT_HEIGHT*0.5 && y <= LAYOUT_HEIGHT*0.9) {
            page = Math.max(0, Math.min((rewards().size()-1)/PAGE_SIZE, page + (delta < 0 ? 1 : -1)));
            return true;
        }
        return false;
    }

    @Override protected boolean canvasClicked(double x, double y, int button) {
        if (button == 0 && x >= MODEL_X && x <= MODEL_X + MODEL_SIZE
                && y >= MODEL_Y && y <= MODEL_Y + MODEL_SIZE) {
            rotation.grab(Util.getMillis());
            return true;
        }
        if (button == 0 && rewards().size() > PAGE_SIZE && y >= LAYOUT_HEIGHT*0.49 && y <= LAYOUT_HEIGHT*0.53) {
            if (x >= LAYOUT_WIDTH*0.88 && x <= LAYOUT_WIDTH*0.91) { page = Math.max(0, page-1); return true; }
            if (x >= LAYOUT_WIDTH*0.94 && x <= LAYOUT_WIDTH*0.97) { page = Math.min((rewards().size()-1)/PAGE_SIZE, page+1); return true; }
        }
        return false;
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!minecraft.isWindowActive()) rotation.release(Util.getMillis());
        rotation.update(Util.getMillis());
        g.fill(LAYOUT_WIDTH*3/100, LAYOUT_HEIGHT*53/100, LAYOUT_WIDTH*97/100, LAYOUT_HEIGHT*53/100+1, 0xFFD3D3D3);
        g.fill(LAYOUT_WIDTH*25/100, LAYOUT_HEIGHT*92/100, LAYOUT_WIDTH*75/100, LAYOUT_HEIGHT*92/100+1, 0xFFD3D3D3);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, MODEL_X, MODEL_Y,
                    rotation.yaw(), rotation.pitch(), preview.box(), minecraft.player, MODEL_SIZE/16F);
        Style bold = Style.EMPTY.withBold(true);
        ClassicBoxUi.centeredText(g, font, Component.translatable("gui.csgobox.csgo_box.title").withStyle(bold),
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.059F, 2, LAYOUT_WIDTH*0.9F, 0xFFD3D3D3);
        ClassicBoxUi.centeredText(g, font, unlockLabel,
                LAYOUT_WIDTH/2F, LAYOUT_HEIGHT*0.13F, 0.8F, LAYOUT_WIDTH*0.9F, 0xFFD3D3D3);
        label(g, Component.translatable("gui.csgobox.csgo_box.label_items").withStyle(bold), 3, 50.3F, 0.8F);

        hovered = null;
        for (int n = page*PAGE_SIZE; n < Math.min(rewards().size(), (page+1)*PAGE_SIZE); n++) {
            int index = n%PAGE_SIZE, column = index%10, row = index/10;
            int x = LAYOUT_WIDTH*4/100 + column*LAYOUT_WIDTH*9/100, y = LAYOUT_HEIGHT*(row == 0 ? 55 : 73)/100;
            DisplayReward entry = rewards().get(n);
            ClassicBoxUi.previewCard(g, entry.stack(), entry.grade(), x, y, LAYOUT_WIDTH, LAYOUT_HEIGHT, rewardLayouts.get(n));
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
            if (!rotation.isDragging() && mouseX >= x && mouseX < x+LAYOUT_WIDTH*8/100
                    && mouseY >= y && mouseY < y+LAYOUT_HEIGHT*11/100) hovered = entry;
        }
        if (rewards().size() > PAGE_SIZE) {
            label(g, Component.literal("<"), 88, 50.3F, 0.8F);
            label(g, Component.literal((page+1)+"/"+((rewards().size()+PAGE_SIZE-1)/PAGE_SIZE)), 91, 50.3F, 0.6F);
            label(g, Component.literal(">"), 95, 50.3F, 0.8F);
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
        }
    }

    // Vanilla tooltip positioning needs the real screen coordinates, outside the canvas transform.
    @Override protected void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        if (hovered != null) {
            if (hovered.grade() == 5) g.renderTooltip(font, Component.translatable("gui.csgobox.csgo_box.label_gold"), mouseX, mouseY);
            else {
                int context = (hasShiftDown() ? 1 : 0) | (hasControlDown() ? 2 : 0)
                        | (hasAltDown() ? 4 : 0) | (minecraft.options.advancedItemTooltips ? 8 : 0);
                ItemStack stack = hovered.stack();
                List<Component> lines = tooltip.get(hovered, context, Util.getMillis(),
                        () -> List.copyOf(getTooltipFromItem(minecraft, stack)));
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
