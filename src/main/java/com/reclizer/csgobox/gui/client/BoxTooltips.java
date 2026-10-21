package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.item.BoxCatalog;
import com.reclizer.csgobox.item.ItemCsgoBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CsgoBox.MODID, value = Dist.CLIENT)
public final class BoxTooltips {
    private BoxTooltips() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ItemCsgoBox)) return;
        BoxCatalog.appendRewardTooltip(ItemCsgoBox.boxId(event.getItemStack()), Screen.hasShiftDown(), event.getToolTip());
    }
}
