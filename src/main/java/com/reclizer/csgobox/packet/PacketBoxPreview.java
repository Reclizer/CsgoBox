package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.gui.client.ClientPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record PacketBoxPreview(UUID token, ItemStack box, ItemStack key, List<DisplayReward> rewards) {
    public PacketBoxPreview(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readItem(), buf.readItem(), buf.readList(DisplayReward::new));
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(token); buf.writeItem(box); buf.writeItem(key);
        buf.writeCollection(rewards, (buffer, reward) -> reward.write(buffer));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.preview(this)));
        context.setPacketHandled(true);
    }
}
