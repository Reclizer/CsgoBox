package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.capability.ModCapability;
import com.reclizer.csgobox.capability.csbox.ICsboxCap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpdateMode {

    public long seed;
    public int mode;
    public PacketUpdateMode(FriendlyByteBuf buf) {
        mode=buf.readInt();
    }

    //Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(mode);
    }

    public PacketUpdateMode(int mode) {
        this.mode=mode;
    }



    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null) {
                ICsboxCap iCsboxCap= ModCapability.getSeed(ctx.get().getSender()).orElse(null);
                if(iCsboxCap==null){
                    return;
                }
                iCsboxCap.setMode(mode);

            }
        });
        ctx.get().setPacketHandled(true);
    }
}
