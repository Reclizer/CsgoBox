package com.reclizer.csgobox.capability;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.capability.csbox.CsboxCapAttacher;
import com.reclizer.csgobox.capability.csbox.ICsboxCap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModCapability {

    public static final Capability<ICsboxCap> PLAYER_SEED = CapabilityManager.get(new CapabilityToken<>() {
    });



    public static LazyOptional<ICsboxCap> getSeed(final LivingEntity entity) {
        if (entity == null)
            return LazyOptional.empty();
        return entity.getCapability(PLAYER_SEED);
    }






    @SuppressWarnings("unused")
    @Mod.EventBusSubscriber(modid = CsgoBox.MODID)
    public static class EventHandler {

        @SubscribeEvent
        public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                CsboxCapAttacher.attach(event);
            }
        }

        @SubscribeEvent
        public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(ICsboxCap.class);
        }


        /**
         * Copy the player's mana when they respawn after dying or returning from the end.
         *
         * @param event The event
         */



        @SubscribeEvent
        public static void playerClone(PlayerEvent.Clone event) {
            Player oldPlayer = event.getOriginal();
            oldPlayer.revive();
            event.getOriginal().invalidateCaps();
        }







    }





}
