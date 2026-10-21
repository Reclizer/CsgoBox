package com.reclizer.csgobox.utils;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/** Optional effect owned by a box screen; never replaces another mod's effect. */
public final class BlurHandler {
    private static final ResourceLocation EFFECT = new ResourceLocation("shaders/post/fade_in_blur.json");
    private static PostChain owned;
    private static boolean attempted;
    private BlurHandler() {}

    public static void updateShader(boolean close) {
        Minecraft minecraft = Minecraft.getInstance();
        var renderer = minecraft.gameRenderer;
        if (close || minecraft.level == null || !com.reclizer.csgobox.config.ClientConfig.BLUR.get()) {
            if (owned != null && renderer.currentEffect() == owned) renderer.shutdownEffect();
            owned = null;
            attempted = false;
            return;
        }
        if (attempted || renderer.currentEffect() != null) return;
        attempted = true;
        try {
            renderer.loadEffect(EFFECT);
            owned = renderer.currentEffect();
        } catch (RuntimeException ex) {
            CsgoBox.LOGGER.warn("Optional box blur is unavailable; using the plain background", ex);
        }
    }
}
