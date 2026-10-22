package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.compat.jei.AcquisitionRecipe;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.server.BoxService;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import io.netty.buffer.Unpooled;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class JeiDataGameTests {
    private static AcquisitionSummary fixture() {
        var grid = new ArrayList<>(Collections.nCopies(9,"")); grid.set(1,"minecraft:stone.withTags{sampled:1b}");
        var rules = new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true,grid,2),
                new AcquisitionDefinition.Drop(true,.125F,List.of("minecraft:zombie","minecraft:skeleton"),3),
                new AcquisitionDefinition.Trade(true,"minecraft:weaponsmith",2,"minecraft:emerald.withCount(8)","minecraft:diamond.withCount(2)",1,9,4));
        return new AcquisitionSummary("csgobox:jei_test","minecraft:tripwire_hook.withTags{sampled:1b}",rules,rules);
    }
    @GameTest(template="empty")
    public static void publicAcquisitionCodecPreservesAllRulesAndNbt(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var summary = fixture(); new PacketAcquisition(summary).toBytes(buffer);
            helper.assertTrue(summary.equals(new PacketAcquisition(buffer).summary()) && buffer.readableBytes()==0,
                    "Acquisition sync lost rules, counts or sampled NBT");
        } finally { buffer.release(); }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void acquisitionRecipeDataCoversAllMethodsAndItemVariants(GameTestHelper helper) {
        var summary = fixture(); var entries = AcquisitionRecipe.build(List.of(summary));
        helper.assertTrue(entries.size()==8,"Missing box/key acquisition methods or entity sources");
        var craft = entries.stream().filter(r -> r.kind()==AcquisitionRecipe.Kind.CRAFTING).findFirst().orElseThrow();
        helper.assertTrue(craft.inputs().size()==9 && craft.inputs().get(1).hasTag() && craft.output().getCount()==2,
                "Craft display lost full NBT or output count");
        var trade = entries.stream().filter(r -> r.kind()==AcquisitionRecipe.Kind.TRADE).findFirst().orElseThrow();
        helper.assertTrue(trade.inputs().get(0).getCount()==8 && trade.inputs().get(1).getCount()==2
                && trade.level()==2 && trade.uses()==9 && trade.xp()==4,"Trade display differs from server rules");
        helper.assertTrue(entries.stream().filter(r -> r.kind()==AcquisitionRecipe.Kind.DROP).allMatch(r -> r.chance()==.125F
                && r.output().getCount()==3),"Drop display differs from server rules");
        var empty = AcquisitionRecipe.build(List.of(new AcquisitionSummary(summary.id(),"minecraft:stick",null,null)));
        var changed = AcquisitionRecipe.build(List.of(new AcquisitionSummary(summary.id(),"minecraft:blaze_rod",null,null)));
        helper.assertTrue(empty.size()==2 && empty.stream().allMatch(r -> r.kind()==AcquisitionRecipe.Kind.NONE),"Unobtainable configured items were omitted");
        helper.assertTrue(!empty.get(1).fingerprint().equals(changed.get(1).fingerprint()),"Changing required item reuses stale JEI output");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void previewPagesCountOneLegendarySlotAndPreserveAllEntries(GameTestHelper helper) {
        for (int regular : List.of(19,20,39,255,256)) {
            var entries = new ArrayList<RewardEntry>();
            for (int i=0;i<regular;i++) entries.add(new RewardEntry("minecraft:stone.withTags{ordinal:"+i+"}",1));
            boolean gold = regular < 256;
            if (gold) entries.add(new RewardEntry("minecraft:diamond",5));
            var box = BoxDefinition.create("csgobox:pages","Pages",entries);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BoxService.previewData(UUID.randomUUID(),box).toBytes(buffer); var preview = new PacketBoxPreview(buffer);
                helper.assertTrue(preview.rewards().size()==regular+(gold?1:0),"Paging omitted or duplicated rewards");
                helper.assertTrue(preview.rewards().get(regular-1).stack().getTag().getInt("ordinal")==regular-1,"Last regular reward changed");
                if (gold) helper.assertTrue(preview.rewards().get(regular).grade()==5,"Gold did not occupy exactly one final slot");
                helper.assertTrue((preview.rewards().size()>20)==(regular>=20),"Wrong paging boundary including gold");
            } finally { buffer.release(); }
        } helper.succeed();
    }
    @GameTest(template="empty")
    public static void oversizedAcquisitionDropListsAreRejectedBeforeReadingEntries(GameTestHelper helper) {
        for (int size : List.of(-1,257)) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeUtf("csgobox:test").writeUtf("").writeBoolean(false).writeBoolean(true);
                buffer.writeFloat(.1F); buffer.writeVarInt(1); buffer.writeVarInt(size);
                boolean rejected=false;
                try { new PacketAcquisition(buffer); } catch (IllegalArgumentException expected) { rejected=true; }
                helper.assertTrue(rejected,"Unbounded drop sources accepted");
            } finally { buffer.release(); }
        } helper.succeed();
    }
}
