package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.gui.client.ClientPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record PacketBoxResult(UUID token, ItemStack reward, int grade) {
    public PacketBoxResult(FriendlyByteBuf buf) { this(buf.readUUID(), buf.readItem(), buf.readVarInt()); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeUUID(token); buf.writeItem(reward); buf.writeVarInt(grade); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.result(this)));
        context.setPacketHandled(true);
    }
}
