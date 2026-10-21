package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.server.BoxService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** A one-use open request, containing no client-selected item or key. */
public record PacketCsgoProgress(UUID token) {
    public PacketCsgoProgress(FriendlyByteBuf buf) { this(buf.readUUID()); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeUUID(token); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) BoxService.open(context.getSender(), token);
        });
        context.setPacketHandled(true);
    }
}
