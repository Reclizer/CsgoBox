package com.reclizer.csgobox.gui;


import com.mojang.blaze3d.platform.ScreenManager;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.gui.client.CsboxScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


public class RecModScreens {

    public static void clientLoad() {
        MenuScreens.register(RecModMenus.CSGO_BOX_CRAFT.get(), CsgoBoxCraftScreen::new);
    }
}
