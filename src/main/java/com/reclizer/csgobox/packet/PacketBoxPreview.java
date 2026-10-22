package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.gui.client.ClientPackets;
import com.reclizer.csgobox.config.ConsumptionMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record PacketBoxPreview(UUID token, ItemStack box, ItemStack key, List<DisplayReward> rewards,
                               List<DisplayReward> legendaryRewards, String consumption) {
    public PacketBoxPreview {
        if (!ConsumptionMode.valid(consumption) || ConsumptionMode.parse(consumption).requiresKey() && key.isEmpty())
            throw new IllegalArgumentException("Invalid preview consumption");
        if (rewards.size() > 21 || legendaryRewards.size() > 256
                || rewards.stream().anyMatch(r -> r.stack().isEmpty() || r.grade() < 1 || r.grade() > 5)
                || legendaryRewards.stream().anyMatch(r -> r.stack().isEmpty() || r.grade() != 5))
            throw new IllegalArgumentException("Invalid preview rewards");
        rewards = List.copyOf(rewards);
        legendaryRewards = List.copyOf(legendaryRewards);
    }
    public PacketBoxPreview(UUID token, ItemStack box, ItemStack key, List<DisplayReward> rewards, List<DisplayReward> legendaryRewards) {
        this(token, box, key, rewards, legendaryRewards, key.isEmpty() ? "box" : "both");
    }
    public PacketBoxPreview(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readItem(), buf.readItem(), readRewards(buf, 21), readRewards(buf, 256), buf.readUtf(16));
    }
    private static List<DisplayReward> readRewards(FriendlyByteBuf buf, int limit) {
        int count = buf.readVarInt();
        if (count < 0 || count > limit) throw new IllegalArgumentException("Oversized preview rewards");
        var result = new java.util.ArrayList<DisplayReward>(count);
        for (int i = 0; i < count; i++) result.add(new DisplayReward(buf));
        return result;
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(token); buf.writeItem(box); buf.writeItem(key);
        buf.writeCollection(rewards, (buffer, reward) -> reward.write(buffer));
        buf.writeCollection(legendaryRewards, (buffer, reward) -> reward.write(buffer));
        buf.writeUtf(consumption, 16);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.preview(this)));
        context.setPacketHandled(true);
    }
}
