package com.reclizer.csgobox.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import java.util.Objects;

/** Public tooltip data only: no stack NBT, quantities, weights or gold rewards. */
public record RewardName(Component name, int grade) {
    public RewardName {
        Objects.requireNonNull(name);
        if (grade < 1 || grade > 4) throw new IllegalArgumentException("Invalid public reward grade");
        name = name.copy();
    }
    public RewardName(FriendlyByteBuf buf) { this(buf.readComponent(), buf.readVarInt()); }
    public void write(FriendlyByteBuf buf) { buf.writeComponent(name); buf.writeVarInt(grade); }
}
