package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ReelMotion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The original reel presentation is cosmetic; the server has already delivered the reward. */
public class CsboxProgressScreen extends BoxScreen {
    private static final int LEAD_IN = 4;
    private final List<DisplayReward> reel = new ArrayList<>();
    private final ItemStack reward;
    private final int grade;
    private final ReelMotion motion;
    private int ticks;
    private int lastSoundIndex = -1;

    public CsboxProgressScreen(List<DisplayReward> display, ItemStack reward, int grade) {
        super(Component.translatable("gui.csgobox.csgo_box.title"));
        this.reward = reward.copy();
        this.grade = grade;
        List<DisplayReward> safe = display.isEmpty() ? List.of(new DisplayReward(reward, grade)) : display;
        Random random = new Random();
        for (int i = 0; i < 50; i++) reel.add(safe.get(random.nextInt(safe.size())));
        reel.set(ReelMotion.WINNER, new DisplayReward(this.reward, grade));
        motion = new ReelMotion(random.nextFloat(0.935F, 1.11F), random.nextFloat(0.12F, 0.88F));
    }

    @Override public void tick() {
        super.tick();
        if (minecraft.screen != this) return;
        ticks++;
        int index = motion.underPointer(ticks - LEAD_IN);
        if (index >= 0 && index != lastSoundIndex && minecraft.player != null)
            minecraft.player.playSound(ModSounds.CS_DITA.get(), 0.6F, 1);
        lastSoundIndex = index;
        if (ticks >= LEAD_IN + ReelMotion.DURATION) minecraft.setScreen(new CsLookItemScreen(reward, grade));
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (ticks < LEAD_IN) return;
        float time = ticks - LEAD_IN + partialTick;
        float y = LAYOUT_HEIGHT * 0.37F;
        for (int i = 0; i < reel.size(); i++) {
            float x = motion.left(i, time) * LAYOUT_WIDTH;
            if (x + LAYOUT_WIDTH * ReelMotion.CARD_WIDTH < 0 || x > LAYOUT_WIDTH) continue;
            var entry = reel.get(i);
            ClassicBoxUi.reelCard(g, entry.stack(), entry.grade(), x, y, LAYOUT_WIDTH, LAYOUT_HEIGHT);
        }
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 300);
            g.fill(LAYOUT_WIDTH/2, LAYOUT_HEIGHT*37/100, LAYOUT_WIDTH/2+2, LAYOUT_HEIGHT*37/100+LAYOUT_HEIGHT*25/100+2, 0x80FFD700);
        } finally { g.pose().popPose(); }
        ClassicBoxUi.reelMask(g, LAYOUT_WIDTH, LAYOUT_HEIGHT);
    }
}
