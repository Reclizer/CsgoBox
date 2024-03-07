package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class PacketGiveItem {

    private String item;
    public PacketGiveItem(FriendlyByteBuf buf) {
        item=buf.readUtf();
    }

    //Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(item);
    }

    public PacketGiveItem(String item) {
        this.item=item;
    }



    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null) {


                if(ctx.get().getSender() instanceof Player){
                    //System.out.println(item);
                    ItemStack giveItem= ItemNBT.getStacks(item);




                    ServerPlayer player=(ServerPlayer) ctx.get().getSender();



                    if(player.getInventory()!=null&&giveItem!=null){
                        player.getInventory().add(giveItem);
                    }

                }

            }
        });
        ctx.get().setPacketHandled(true);
    }
}
