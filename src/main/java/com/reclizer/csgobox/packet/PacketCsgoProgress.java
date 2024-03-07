package com.reclizer.csgobox.packet;


import com.reclizer.csgobox.gui.client.CsboxProgressScreen;
import com.reclizer.csgobox.item.ItemCsgoBox;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

public class PacketCsgoProgress {
    private final int buttonID;
    private String item;
    public PacketCsgoProgress(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();

        this.item=buffer.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(buttonID);

        buf.writeUtf(item);
    }


    public PacketCsgoProgress(int buttonID,String item) {
        this.buttonID = buttonID;

        this.item=item;
    }



    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        ctx.get().enqueueWork(() -> {
            Player entity = context.getSender();
            int buttonID = this.buttonID;

            Level world = entity.level();


            if (buttonID == 1) {

                //Minecraft.getInstance().setScreen(new CsboxProgressScreen());
            }
            if (buttonID == 2) {
            if(entity.getMainHandItem().getItem() instanceof ItemCsgoBox){
                entity.getMainHandItem().shrink(1);
                for (ItemStack stack : entity.getInventory().items) {

                    if (Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString().equals(item) ) {
                        stack.shrink(1);

                    }
                }
            }




            }
        });
        ctx.get().setPacketHandled(true);
    }




    public static void execute(LevelAccessor world,  Entity entity) {
        if (entity == null)
            return;
        Random random=new Random();
        if (entity instanceof Player _player)
            _player.closeContainer();

    }



}
