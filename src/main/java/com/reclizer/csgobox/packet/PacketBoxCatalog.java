package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.gui.client.ClientPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Supplier;

/** Creative-tab identities, without loot tables. */
public record PacketBoxCatalog(List<String> names) {
    public PacketBoxCatalog(FriendlyByteBuf buf) { this(buf.readList(buffer -> buffer.readUtf(128))); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeCollection(names, (buffer, name) -> buffer.writeUtf(name, 128)); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.catalog(this)));
        context.setPacketHandled(true);
    }
}
