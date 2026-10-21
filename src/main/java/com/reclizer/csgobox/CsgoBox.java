package com.reclizer.csgobox;

import com.mojang.logging.LogUtils;
import com.reclizer.csgobox.gui.RecModMenus;
import com.reclizer.csgobox.gui.RecModScreens;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.packet.Networking;
import com.reclizer.csgobox.sounds.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CsgoBox.MODID)
public class CsgoBox {
    public static final String MODID = "csgobox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CsgoBox() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        ModSounds.SOUNDS.register(bus);
        RecModMenus.register(bus);
        ModItems.register(bus);
        ModItems.registerTab(bus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Networking::registerMessages);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(RecModScreens::clientLoad);
        }
    }
}
