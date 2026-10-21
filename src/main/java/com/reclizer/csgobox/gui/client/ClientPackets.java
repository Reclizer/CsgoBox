package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.packet.PacketBoxPreview;
import com.reclizer.csgobox.packet.PacketBoxResult;
import com.reclizer.csgobox.packet.PacketBoxCatalog;
import com.reclizer.csgobox.item.BoxCatalog;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.CsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/** Referenced only from the physical-client packet dispatch. */
@Mod.EventBusSubscriber(modid = CsgoBox.MODID, value = Dist.CLIENT)
public final class ClientPackets {
    private ClientPackets() {}

    public static void preview(PacketBoxPreview packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isAlive()) minecraft.setScreen(new CsboxScreen(packet));
    }

    public static void result(PacketBoxResult packet) {
        Minecraft minecraft = Minecraft.getInstance();
        // A late response cannot reopen a screen the player already closed.
        if (minecraft.player == null || !(minecraft.screen instanceof CsboxScreen screen)
                || !screen.token().equals(packet.token())) return;
        if (packet.reward().isEmpty()) screen.onClose();
        else minecraft.setScreen(new CsboxProgressScreen(screen.rewards(), packet.reward(), packet.grade()));
    }

    public static void catalog(PacketBoxCatalog packet) {
        BoxCatalog.replace(packet.names());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            var parameters = new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(),
                    minecraft.player.canUseGameMasterBlocks(), minecraft.level.registryAccess());
            ModItems.EQUIPMENT_TAB.get().buildContents(parameters);
            CreativeModeTabs.searchTab().buildContents(parameters);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { BoxCatalog.replace(java.util.List.of()); }
}
