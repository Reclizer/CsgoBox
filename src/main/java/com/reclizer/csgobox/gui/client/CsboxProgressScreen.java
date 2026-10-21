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

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        if (ticks < LEAD_IN) return;
        float time = ticks - LEAD_IN + partialTick;
        float y = height * 0.37F;
        g.enableScissor(0, 0, width, height);
        try {
            for (int i = 0; i < reel.size(); i++) {
                float x = motion.left(i, time) * width;
                if (x + width * ReelMotion.CARD_WIDTH < 0 || x > width) continue;
                var entry = reel.get(i);
                ClassicBoxUi.reelCard(g, entry.stack(), entry.grade(), x, y, width, height);
            }
            g.pose().pushPose();
            try {
                g.pose().translate(0, 0, 300);
                g.fill(width/2, height*37/100, width/2+2, height*37/100+height*25/100+2, 0x80FFD700);
            } finally { g.pose().popPose(); }
            ClassicBoxUi.reelMask(g, width, height);
        } finally { g.disableScissor(); }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
