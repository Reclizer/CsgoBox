package com.reclizer.csgobox.compat;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.gui.client.BoxScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** TaCZ's HUD does not respect hideGui; keep it outside the opening composition. */
@Mod.EventBusSubscriber(modid = CsgoBox.MODID, value = Dist.CLIENT)
public final class TaczClientCompat {
    private TaczClientCompat() {}

    @SubscribeEvent
    public static void beforeOverlay(RenderGuiOverlayEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof BoxScreen
                && event.getOverlay().id().getNamespace().equals("tacz")) event.setCanceled(true);
    }
}
