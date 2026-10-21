package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.GuiItemMove;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
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
    private int page;
    private boolean requested;
    private boolean playedSound;
    private Button openButton;
    private float rotationX;
    private float rotationY;
    private boolean rotating;
    private DisplayReward hovered;

    public CsboxScreen(PacketBoxPreview preview) { super(preview.box().getHoverName()); this.preview = preview; }
    public UUID token() { return preview.token(); }
    public List<DisplayReward> rewards() { return preview.rewards(); }

    @Override protected void init() {
        super.init();
        rotating = false;
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
        if (openButton != null) openButton.active = !requested && keyCount() > 0;
    }

    @Override protected boolean canvasDragged(int button, double dx, double dy) {
        if (button != 0 || !rotating) return false;
        rotationX = GuiItemMove.renderRotAngleX(dx, rotationX);
        rotationY = GuiItemMove.renderRotAngleY(dy, rotationY);
        return true;
    }

    @Override protected void canvasReleased(int button) {
        if (button == 0) rotating = false;
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
            rotating = true;
            return true;
        }
        if (button == 0 && rewards().size() > PAGE_SIZE && y >= LAYOUT_HEIGHT*0.49 && y <= LAYOUT_HEIGHT*0.53) {
            if (x >= LAYOUT_WIDTH*0.88 && x <= LAYOUT_WIDTH*0.91) { page = Math.max(0, page-1); return true; }
            if (x >= LAYOUT_WIDTH*0.94 && x <= LAYOUT_WIDTH*0.97) { page = Math.min((rewards().size()-1)/PAGE_SIZE, page+1); return true; }
        }
        return false;
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(LAYOUT_WIDTH*3/100, LAYOUT_HEIGHT*53/100, LAYOUT_WIDTH*97/100, LAYOUT_HEIGHT*53/100+1, 0xFFD3D3D3);
        g.fill(LAYOUT_WIDTH*25/100, LAYOUT_HEIGHT*92/100, LAYOUT_WIDTH*75/100, LAYOUT_HEIGHT*92/100+1, 0xFFD3D3D3);
        if (minecraft.player != null)
            GuiItemMove.renderItemInInventoryFollowsMouse(g, MODEL_X, MODEL_Y,
                    rotationX, rotationY, preview.box(), minecraft.player, MODEL_SIZE/16F);
        Style bold = Style.EMPTY.withBold(true);
        label(g, Component.translatable("gui.csgobox.csgo_box.title").withStyle(bold), 47.6F, 5.9F, 2);
        label(g, Component.translatable("gui.csgobox.csgo_box.label_box"), 46, 13, 0.8F);
        label(g, preview.box().getHoverName(), 50, 13, 0.8F);
        label(g, Component.translatable("gui.csgobox.csgo_box.label_items").withStyle(bold), 3, 50.3F, 0.8F);

        hovered = null;
        for (int n = page*PAGE_SIZE; n < Math.min(rewards().size(), (page+1)*PAGE_SIZE); n++) {
            int index = n%PAGE_SIZE, column = index%10, row = index/10;
            int x = LAYOUT_WIDTH*4/100 + column*LAYOUT_WIDTH*9/100, y = LAYOUT_HEIGHT*(row == 0 ? 55 : 73)/100;
            DisplayReward entry = rewards().get(n);
            ClassicBoxUi.previewCard(g, entry.stack(), entry.grade(), x, y, LAYOUT_WIDTH, LAYOUT_HEIGHT);
            Component name = entry.grade() == 5 ? Component.translatable("gui.csgobox.csgo_box.label_gold") : entry.stack().getHoverName();
            // Long names wrap inside their original card column instead of spilling into adjacent cards.
            int textWidth = Math.max(1, (int)(LAYOUT_WIDTH*0.08F/0.6F));
            int line = 0;
            for (var part : font.split(name, textWidth)) {
                if (line == 2) break;
                g.pose().pushPose();
                try {
                    g.pose().translate(LAYOUT_WIDTH*0.04F + column*LAYOUT_WIDTH*0.09F, LAYOUT_HEIGHT*(row == 0 ? 0.67F : 0.85F) + line*6F, 300);
                    g.pose().scale(0.6F, 0.6F, 1);
                    g.drawString(font, part, 0, 0, 0xFFD3D3D3, false);
                } finally { g.pose().popPose(); }
                line++;
            }
            if (mouseX >= x && mouseX < x+LAYOUT_WIDTH*8/100 && mouseY >= y && mouseY < y+LAYOUT_HEIGHT*11/100) hovered = entry;
        }
        if (rewards().size() > PAGE_SIZE) {
            label(g, Component.literal("<"), 88, 50.3F, 0.8F);
            label(g, Component.literal((page+1)+"/"+((rewards().size()+PAGE_SIZE-1)/PAGE_SIZE)), 91, 50.3F, 0.6F);
            label(g, Component.literal(">"), 95, 50.3F, 0.8F);
        }
        if (!preview.key().isEmpty()) {
            ClassicBoxUi.item(g, preview.key(), LAYOUT_WIDTH*0.25F, LAYOUT_HEIGHT*0.93F, 1);
            int count = keyCount();
            if (count > 0) label(g, Component.literal(" × " + count), 28, 94, 0.8F);
            else {
                label(g, Component.translatable("gui.csgobox.csgo_box.label_open"), 28, 94, 0.8F);
                label(g, preview.key().getHoverName(), 35, 94, 0.8F);
                label(g, Component.translatable("gui.csgobox.csgo_box.label_open_1"), 40, 94, 0.8F);
            }
        }
    }

    // Vanilla tooltip positioning needs the real screen coordinates, outside the canvas transform.
    @Override protected void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        if (hovered != null) {
            if (hovered.grade() == 5) g.renderTooltip(font, Component.translatable("gui.csgobox.csgo_box.label_gold"), mouseX, mouseY);
            else g.renderTooltip(font, hovered.stack(), mouseX, mouseY);
        }
    }

    private void label(GuiGraphics g, Component text, float x, float y, float scale) {
        ClassicBoxUi.text(g, font, text, LAYOUT_WIDTH*x/100, LAYOUT_HEIGHT*y/100, scale, 0xFFD3D3D3);
    }
}
