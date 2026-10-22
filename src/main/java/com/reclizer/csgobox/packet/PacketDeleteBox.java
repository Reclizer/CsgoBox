package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.server.EditorDeleteBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record PacketDeleteBox(int containerId, int requestId, String id, String revision) {
    public PacketDeleteBox(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readUtf(64)); }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(requestId); buf.writeUtf(id,128); buf.writeUtf(revision,64);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(() -> {
            var player=context.getSender();
            if (player==null) return;
            try {
                String error=EditorDeleteBox.delete(player,containerId,requestId,id,revision);
                Networking.sendToPlayerClient(PacketEditorState.deleteResult(containerId,requestId,error),player);
            } catch (java.io.IOException | RuntimeException ex) {
                CsgoBox.LOGGER.warn("Cannot delete editor pool {}", id, ex);
                Networking.sendToPlayerClient(PacketEditorState.failure(containerId,requestId,ex.getMessage()),player);
            }
        });
        context.setPacketHandled(true);
    }
}
