package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.io.IOException;
import java.util.function.Supplier;

/** Only the menu ID and name cross the wire; items come from server-owned slots. */
public record PacketSaveBox(int containerId, String name) {
    public PacketSaveBox(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readUtf(128)); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeVarInt(containerId); buf.writeUtf(name, 128); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.hasPermissions(2) || !player.isAlive()
                    || !(player.containerMenu instanceof CsgoBoxCraftMenu menu)
                    || menu.containerId != containerId || !menu.stillValid(player)) return;
            try {
                CsgoBoxManage.save(name.trim(), menu.rewards());
                com.reclizer.csgobox.server.BoxService.clearSessions();
                player.server.getPlayerList().getPlayers().forEach(com.reclizer.csgobox.server.BoxService::syncCatalog);
                player.sendSystemMessage(Component.translatable("message.csgobox.saved", name.trim()));
                player.closeContainer();
            } catch (IOException | IllegalArgumentException ex) {
                CsgoBox.LOGGER.warn("Cannot save box {}", name, ex);
                player.sendSystemMessage(Component.translatable("message.csgobox.config_error", ex.getMessage()));
            }
        });
        context.setPacketHandled(true);
    }
}
