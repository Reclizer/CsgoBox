package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ColorTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Cosmetic reel only. The server has already delivered the immutable result. */
public class CsboxProgressScreen extends BoxScreen {
    private static final int WINNER_INDEX = 45;
    private static final int DURATION = 145;
    private final List<DisplayReward> reel = new ArrayList<>();
    private final ItemStack reward;
    private final int grade;
    private int ticks;
    private int lastSoundIndex = -1;

    public CsboxProgressScreen(List<DisplayReward> display, ItemStack reward, int grade) {
        super(Component.translatable("gui.csgobox.csgo_box.title"));
        this.reward = reward.copy();
        this.grade = grade;
        List<DisplayReward> safe = display.isEmpty() ? List.of(new DisplayReward(reward, grade)) : display;
        Random random = new Random();
        for (int i = 0; i < 50; i++) reel.add(safe.get(random.nextInt(safe.size())));
        reel.set(WINNER_INDEX, new DisplayReward(this.reward, grade));
    }

    private float position(float time) {
        float t = Math.max(0, Math.min(1, time / DURATION));
        return WINNER_INDEX * (1 - (float) Math.pow(1 - t, 4));
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft.screen != this) return;
        ticks++;
        int index = (int) position(ticks);
        if (index != lastSoundIndex && minecraft.player != null) {
            minecraft.player.playSound(ModSounds.CS_DITA.get(), 0.6F, 1);
            lastSoundIndex = index;
        }
        if (ticks >= DURATION) minecraft.setScreen(new CsLookItemScreen(reward, grade));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 30, 0xFFFFFF);
        float position = position(ticks + partialTick);
        int spacing = Math.max(44, width / 5);
        int cardWidth = spacing - 8;
        int y = height / 2 - 34;
        graphics.enableScissor(0, y - 2, width, y + 72);
        try {
            for (int i = Math.max(0, (int) position - 4); i < Math.min(reel.size(), (int) position + 5); i++) {
                var entry = reel.get(i);
                int x = Math.round(width / 2F + (i - position) * spacing - cardWidth / 2F);
                graphics.fill(x, y, x + cardWidth, y + 68, 0xCC373E4A);
                graphics.fill(x, y + 65, x + cardWidth, y + 68, ColorTools.colorItems(entry.grade()));
                if (entry.grade() == 5) graphics.drawCenteredString(font, "?", x + cardWidth / 2, y + 25, 0xFFD700);
                else CsboxScreen.renderScaledItem(graphics, entry.stack(), x + cardWidth / 2 - 16, y + 14, 2);
            }
        } finally { graphics.disableScissor(); }
        graphics.fill(width / 2 - 1, y - 8, width / 2 + 1, y + 76, 0xFFFFD700);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
