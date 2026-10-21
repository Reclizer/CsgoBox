package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;

public class CsboxScreen extends BoxScreen {
    private static final int PAGE_SIZE = 20;
    private final PacketBoxPreview preview;
    private int page;
    private boolean requested;
    private boolean playedSound;
    private Button openButton;

    public CsboxScreen(PacketBoxPreview preview) { super(preview.box().getHoverName()); this.preview = preview; }
    public UUID token() { return preview.token(); }
    public List<DisplayReward> rewards() { return preview.rewards(); }

    @Override
    protected void init() {
        super.init();
        if (!playedSound && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_OPEN.get(), 1, 1);
            playedSound = true;
        }
        openButton = addRenderableWidget(Button.builder(Component.translatable("gui.csgobox.csgo_box.open_box"), button -> {
            if (!requested) {
                requested = true;
                button.active = false;
                Networking.INSTANCE.sendToServer(new PacketCsgoProgress(token()));
            }
        }).bounds(width / 2 - 105, height - 30, 100, 20).build());
        openButton.active = !requested;
        addRenderableWidget(Button.builder(Component.translatable("gui.csgobox.csgo_box.back_box"), button -> onClose())
                .bounds(width / 2 + 5, height - 30, 100, 20).build());
        if (rewards().size() > PAGE_SIZE) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> { page = Math.max(0, page - 1); })
                    .bounds(12, height - 30, 25, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> { page = Math.min((rewards().size() - 1) / PAGE_SIZE, page + 1); })
                    .bounds(width - 37, height - 30, 25, 20).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.player != null && openButton != null)
            openButton.active = !requested && minecraft.player.getInventory().items.stream()
                    .anyMatch(stack -> !stack.isEmpty() && stack.is(preview.key().getItem()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        renderScaledItem(graphics, preview.box(), width / 2 - 24, 38, 3);
        graphics.drawCenteredString(font, Component.translatable("gui.csgobox.csgo_box.label_items"), width / 2, 98, 0xC5CBD3);
        int cellWidth = Math.max(20, (width - 24) / 10);
        int cellHeight = Math.max(32, (height - 162) / 2);
        for (int n = page * PAGE_SIZE; n < Math.min(rewards().size(), (page + 1) * PAGE_SIZE); n++) {
            DisplayReward reward = rewards().get(n);
            int local = n % PAGE_SIZE;
            int x = 12 + local % 10 * cellWidth;
            int y = 114 + local / 10 * cellHeight;
            graphics.fill(x, y, x + cellWidth - 3, y + cellHeight - 3, 0xBB303641);
            graphics.fill(x, y + cellHeight - 5, x + cellWidth - 3, y + cellHeight - 3, ColorTools.colorItems(reward.grade()));
            if (reward.grade() == 5) graphics.drawCenteredString(font, "?", x + (cellWidth - 3) / 2, y + 9, 0xFFD700);
            else graphics.renderItem(reward.stack(), x + (cellWidth - 19) / 2, y + 5);
            if (mouseX >= x && mouseX < x + cellWidth - 3 && mouseY >= y && mouseY < y + cellHeight - 3) {
                if (reward.grade() == 5)
                    graphics.renderTooltip(font, Component.translatable("gui.csgobox.csgo_box.label_gold"), mouseX, mouseY);
                else graphics.renderTooltip(font, reward.stack(), mouseX, mouseY);
            }
        }
        graphics.drawCenteredString(font, Component.translatable("gui.csgobox.required_key", preview.key().getHoverName()),
                width / 2, height - 44, 0xC5CBD3);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    static void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.renderItem(stack, 0, 0);
        } finally { graphics.pose().popPose(); }
    }
}
