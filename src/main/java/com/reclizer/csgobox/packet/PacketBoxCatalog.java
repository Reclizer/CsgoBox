package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.gui.client.ClientPackets;
import com.reclizer.csgobox.item.BoxCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

/** Creative-tab identities, without loot tables. */
public record PacketBoxCatalog(List<BoxCatalog.Entry> entries) {
    public PacketBoxCatalog(FriendlyByteBuf buf) { this(readEntries(buf)); }
    private static List<BoxCatalog.Entry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 512) throw new IllegalArgumentException("Oversized pool catalog");
        var entries = new java.util.ArrayList<BoxCatalog.Entry>(size);
        for (int i = 0; i < size; i++) entries.add(new BoxCatalog.Entry(buf.readUtf(128), buf.readUtf(128)));
        return List.copyOf(entries);
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(entries, (buffer, entry) -> { buffer.writeUtf(entry.id(), 128); buffer.writeUtf(entry.name(), 128); });
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.catalog(this)));
        context.setPacketHandled(true);
    }
}
