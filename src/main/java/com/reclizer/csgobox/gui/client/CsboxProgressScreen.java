package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.ReelMotion;
import com.reclizer.csgobox.utils.ReelPresentation;
import com.reclizer.csgobox.utils.GuiItemMove;
import com.reclizer.csgobox.compat.TaczCompat;
import com.reclizer.csgobox.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The original reel presentation is cosmetic; the server has already delivered the reward. */
public class CsboxProgressScreen extends BoxScreen {
    private final ReelLayer layer = new ReelLayer();
    private final List<DisplayReward> reel = new ArrayList<>();
    private final ItemStack reward;
    private final int grade;
    private final ReelMotion motion;
    private final List<CardItemLayout> layouts;
    private final CardItemLayout rewardLayout;
    private int ticks;
    private int lastSoundIndex = -1;

    public CsboxProgressScreen(List<DisplayReward> display, ItemStack reward, int grade) {
        super(Component.translatable("gui.csgobox.csgo_box.title"));
        this.reward = reward.copy();
        this.grade = grade;
        List<DisplayReward> safe = display.isEmpty() ? List.of(new DisplayReward(reward, grade)) : display;
        Random random = new Random();
        for (int i = 0; i < ReelMotion.CARD_COUNT; i++) reel.add(safe.get(random.nextInt(safe.size())));
        reel.set(ReelMotion.WINNER, new DisplayReward(this.reward, grade));
        var cached = new java.util.IdentityHashMap<ItemStack, CardItemLayout>();
        layouts = reel.stream().map(entry -> cached.computeIfAbsent(entry.stack(), CardItemLayout::of)).toList();
        rewardLayout = layouts.get(ReelMotion.WINNER);
        motion = new ReelMotion(random.nextFloat(), random.nextFloat(0.12F, 0.88F));
        lastSoundIndex = motion.underPointer(0);
    }

    @Override public void tick() {
        super.tick();
        if (minecraft.screen != this) return;
        ticks++;
        int index = motion.underPointer(ticks);
        if (ticks <= ReelMotion.DURATION
                && index >= 0 && index != lastSoundIndex && minecraft.player != null)
            minecraft.player.playSound(ModSounds.CS_DITA.get(), 0.6F, 1);
        lastSoundIndex = index;
        if (ticks >= ReelPresentation.END)
            showNext(new CsLookItemScreen(reward, grade));
    }

    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float elapsed = ticks + partialTick;
        float reveal = ReelPresentation.reveal(elapsed);
        float y = LAYOUT_HEIGHT * 0.37F;
        // The populated strip starts moving immediately; only the final reveal fades it out.
        float opacity = 1-reveal;
        layer.render(g, LAYOUT_WIDTH, LAYOUT_HEIGHT, opacity, true, () -> {
            for (int i = 0; i < reel.size(); i++) {
                float x = motion.left(i, elapsed) * LAYOUT_WIDTH;
                if (x + LAYOUT_WIDTH * ReelMotion.CARD_WIDTH < 0 || x > LAYOUT_WIDTH) continue;
                var entry = reel.get(i);
                ClassicBoxUi.reelCard(g, entry.stack(), entry.grade(), x, y, LAYOUT_WIDTH, LAYOUT_HEIGHT,
                        i == ReelMotion.WINNER ? reveal : 0, layouts.get(i));
            }
        });
        renderWindowOverlay(g, viewport -> ReelFocus.render(g, LAYOUT_WIDTH, LAYOUT_HEIGHT, opacity,
                (float) (Math.hypot(width, height) / viewport.scale())));
        // Draw the pointer AFTER the reel and spotlight so it is never masked or dimmed.
        int pointerAlpha = Math.round(255 * (1-ReelPresentation.smooth(reveal*3)));
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            g.fill(LAYOUT_WIDTH/2, LAYOUT_HEIGHT*37/100-1, LAYOUT_WIDTH/2+1,
                    LAYOUT_HEIGHT*37/100+LAYOUT_HEIGHT*25/100+2, (pointerAlpha << 24) | 0xFFE06B);
        } finally { g.pose().popPose(); }
        if (reveal > 0 && minecraft.player != null) {
            float w = LAYOUT_WIDTH*18/100, h = LAYOUT_HEIGHT*25/100;
            float cx = ReelPresentation.mix(motion.left(ReelMotion.WINNER, elapsed)*LAYOUT_WIDTH+w/2,
                    CsLookItemScreen.MODEL_CENTER_X, reveal);
            float cy = ReelPresentation.mix(y+h/2, CsLookItemScreen.MODEL_CENTER_Y, reveal);
            float startSize = rewardLayout.size(w, h);
            // FIXED gun models are wider than their nominal GUI model square.
            if (TaczCompat.isGun(reward) && ClientConfig.TACZ_3D_PREVIEW.get()) startSize /= 3;
            float size = ReelPresentation.mix(startSize, CsLookItemScreen.MODEL_SIZE, reveal);
            layer.render(g, LAYOUT_WIDTH, LAYOUT_HEIGHT, reveal, false,
                    () -> GuiItemMove.renderItemInInventoryFollowsMouse(g, cx-size/2, cy-size/2,
                            0, 0, reward, minecraft.player, size/16));
        }
    }

    @Override public void removed() {
        try { layer.close(); }
        finally {
            if (minecraft != null) minecraft.getMainRenderTarget().bindWrite(true);
            super.removed();
        }
    }
}
