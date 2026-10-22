package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.client.ClientAcquisitions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

/** One bounded public acquisition summary per pool, in catalog order. */
public record PacketAcquisition(AcquisitionSummary summary) {
    public PacketAcquisition(FriendlyByteBuf buf) {
        this(new AcquisitionSummary(buf.readUtf(128), buf.readUtf(8192), readRules(buf), readRules(buf)));
    }
    private static AcquisitionDefinition readRules(FriendlyByteBuf buf) {
        AcquisitionDefinition.Crafting craft = null;
        AcquisitionDefinition.Drop drop = null;
        AcquisitionDefinition.Trade trade = null;
        if (buf.readBoolean()) {
            var ingredients = new ArrayList<String>();
            for (int i = 0; i < 9; i++) ingredients.add(buf.readUtf(8192));
            craft = new AcquisitionDefinition.Crafting(true, ingredients, buf.readVarInt());
        }
        if (buf.readBoolean()) {
            float chance = buf.readFloat(); int count = buf.readVarInt(), size = buf.readVarInt();
            if (size < 0 || size > 256) throw new IllegalArgumentException("Oversized drop sources");
            var entities = new ArrayList<String>();
            for (int i = 0; i < size; i++) entities.add(buf.readUtf(128));
            drop = new AcquisitionDefinition.Drop(true, chance, entities, count);
        }
        if (buf.readBoolean()) trade = new AcquisitionDefinition.Trade(true, buf.readUtf(128), buf.readVarInt(),
                buf.readUtf(8192), buf.readUtf(8192), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        return new AcquisitionDefinition(craft, drop, trade);
    }
    private static void writeRules(FriendlyByteBuf buf, AcquisitionDefinition rules) {
        var craft = rules.crafting(); buf.writeBoolean(craft.enabled());
        if (craft.enabled()) { for (String item : craft.ingredients()) buf.writeUtf(item, 8192); buf.writeVarInt(craft.count()); }
        var drop = rules.drop(); buf.writeBoolean(drop.enabled());
        if (drop.enabled()) {
            buf.writeFloat(drop.chance()); buf.writeVarInt(drop.count()); buf.writeVarInt(drop.entities().size());
            for (String entity : drop.entities()) buf.writeUtf(entity, 128);
        }
        var trade = rules.trade(); buf.writeBoolean(trade.enabled());
        if (trade.enabled()) {
            buf.writeUtf(trade.profession(), 128); buf.writeVarInt(trade.level());
            buf.writeUtf(trade.cost(), 8192); buf.writeUtf(trade.secondCost(), 8192);
            buf.writeVarInt(trade.count()); buf.writeVarInt(trade.maxUses()); buf.writeVarInt(trade.xp());
        }
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(summary.id(), 128); buf.writeUtf(summary.key(), 8192);
        writeRules(buf, summary.boxRules()); writeRules(buf, summary.keyRules());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAcquisitions.accept(summary)));
        context.setPacketHandled(true);
    }
}
