package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.server.BoxService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** Keeps a still-valid, held preview alive while browsing nested inspection screens. */
public record PacketPreviewHeartbeat(UUID token) {
    public PacketPreviewHeartbeat(FriendlyByteBuf buffer) { this(buffer.readUUID()); }
    public void toBytes(FriendlyByteBuf buffer) { buffer.writeUUID(token); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) BoxService.keepPreviewAlive(player, token);
        });
        context.setPacketHandled(true);
    }
}
