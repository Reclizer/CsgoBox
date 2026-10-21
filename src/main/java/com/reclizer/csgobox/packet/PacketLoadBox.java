package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record PacketLoadBox(int containerId, int requestId, String name) {
    public PacketLoadBox(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt(), buf.readUtf(128)); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeVarInt(containerId); buf.writeVarInt(requestId); buf.writeUtf(name, 128); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.hasPermissions(2) || !player.isAlive()
                    || !(player.containerMenu instanceof CsgoBoxCraftMenu menu)
                    || menu.containerId != containerId || !menu.stillValid(player)) return;
            try {
                var box = CsgoBoxManage.readForEditing(name);
                Networking.sendToPlayerClient(box == null
                        ? PacketEditorState.failure(containerId, requestId, "message.csgobox.unknown_box")
                        : PacketEditorState.success(containerId, requestId, false, box), player);
            } catch (java.io.IOException ex) {
                Networking.sendToPlayerClient(PacketEditorState.failure(containerId, requestId, ex.getMessage()), player);
            }
        });
        context.setPacketHandled(true);
    }
}
