package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.server.EditorTestBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** The client identifies a saved pool; it never supplies the item to be granted. */
public record PacketTestBox(int containerId, int requestId, String name, String revision) {
    public PacketTestBox(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readUtf(64));
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(requestId); buf.writeUtf(name, 128); buf.writeUtf(revision, 64);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            String error = EditorTestBox.give(player, containerId, requestId, name, revision);
            Networking.sendToPlayerClient(PacketEditorState.testResult(containerId, requestId, error), player);
        });
        context.setPacketHandled(true);
    }
}
