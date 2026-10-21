package com.reclizer.csgobox.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Display data only: never accepted as a reward from a client. */
public record DisplayReward(ItemStack stack, int grade) {
    public DisplayReward(FriendlyByteBuf buf) { this(buf.readItem(), buf.readVarInt()); }
    public void write(FriendlyByteBuf buf) { buf.writeItem(stack); buf.writeVarInt(grade); }
}
