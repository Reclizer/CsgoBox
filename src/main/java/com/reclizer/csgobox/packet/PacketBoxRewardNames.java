package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.item.BoxCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** One pool per packet, sent after the identity catalog on login/save/reload. */
public record PacketBoxRewardNames(String id, List<RewardName> rewards) {
    public PacketBoxRewardNames {
        if (!BoxDefinition.validId(id) || rewards.size() > 256)
            throw new IllegalArgumentException("Invalid reward-name summary");
        rewards = List.copyOf(rewards);
    }
    public PacketBoxRewardNames(FriendlyByteBuf buf) { this(buf.readUtf(128), readRewards(buf)); }
    private static List<RewardName> readRewards(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("Oversized reward-name summary");
        var rewards = new ArrayList<RewardName>(count);
        for (int i = 0; i < count; i++) rewards.add(new RewardName(buf));
        return rewards;
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(id, 128);
        buf.writeCollection(rewards, (buffer, reward) -> reward.write(buffer));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BoxCatalog.replaceRewardNames(id, rewards)));
        context.setPacketHandled(true);
    }
}
