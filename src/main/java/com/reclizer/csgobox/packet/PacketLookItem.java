package com.reclizer.csgobox.packet;


import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.NetworkEvent;


import java.util.function.Supplier;

public class PacketLookItem {
    private final int buttonID;

    public PacketLookItem(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();

    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(buttonID);

    }

    public PacketLookItem(int buttonID, int x, int y, int z) {
        this.buttonID = buttonID;

    }

    public PacketLookItem(int buttonID) {
        this.buttonID = buttonID;

    }



    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        ctx.get().enqueueWork(() -> {
            Player entity = context.getSender();
            int buttonID = this.buttonID;

            Level world = entity.level();


            if (buttonID == 1) {

            }
        });
        ctx.get().setPacketHandled(true);
    }


    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return;
        if (entity instanceof Player _player)
            _player.closeContainer();


    }
}
