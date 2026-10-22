package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.server.BoxService;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import io.netty.buffer.Unpooled;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class InspectionGameTests {
    @GameTest(template = "empty")
    public static void previewIncludesAllRegularAndLegendaryIconsWithoutChangingTooltip(GameTestHelper helper) {
        var entries = new ArrayList<RewardEntry>();
        for (int i = 0; i < 25; i++) entries.add(new RewardEntry("minecraft:stone", 1));
        entries.add(new RewardEntry("minecraft:netherite_sword.withTags{display:{Name:'{\"text\":\"Gold sword\"}'},Enchantments:[{id:\"minecraft:sharpness\",lvl:4s}]}", 5));
        entries.add(new RewardEntry("minecraft:diamond_axe", 5));
        var box = BoxDefinition.create("csgobox:inspect-test", "Inspection", entries);
        var token = UUID.randomUUID();
        var packet = BoxService.previewData(token, box);
        helper.assertTrue(packet.rewards().size() == 26 && packet.rewards().get(25).grade() == 5, "Main preview lost regular rewards or mystery badge");
        helper.assertTrue(packet.legendaryRewards().size() == 2 && packet.legendaryRewards().get(0).stack().is(Items.NETHERITE_SWORD)
                && packet.legendaryRewards().get(1).stack().is(Items.DIAMOND_AXE), "Nested list did not include real legendary icons");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.toBytes(buffer);
            var decoded = new PacketBoxPreview(buffer);
            helper.assertTrue(token.equals(decoded.token()) && decoded.legendaryRewards().size() == 2 && decoded.rewards().size() == 26, "Preview codec lost navigation data");
            helper.assertTrue(ItemStack.matches(ItemNBT.parseStrict(box.grade5().get(0)), decoded.legendaryRewards().get(0).stack()), "Legendary NBT was not preserved for inspection");
            helper.assertTrue(buffer.readableBytes() == 0, "Preview codec left unread bytes");
        } finally { buffer.release(); }
        helper.assertTrue(BoxService.rewardNames(box).rewards().size() == 25, "New legendary list changed the non-gold Shift tooltip policy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void goldOnlyAndLargeListsRoundTripWhileInvalidCountsAreRejected(GameTestHelper helper) {
        var allGold = BoxDefinition.create("csgobox:inspect-gold", "Gold", java.util.stream.IntStream.range(0, 256)
                .mapToObj(i -> new RewardEntry("minecraft:diamond", 5)).toList());
        var packet = BoxService.previewData(UUID.randomUUID(), allGold);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.toBytes(buffer);
            var decoded = new PacketBoxPreview(buffer);
            helper.assertTrue(decoded.legendaryRewards().size() == 256 && decoded.rewards().size() == 1, "Gold-only or large list truncated");
        } finally { buffer.release(); }
        var noGold = BoxService.previewData(UUID.randomUUID(), BoxDefinition.create("csgobox:inspect-plain", "Plain", List.of(new RewardEntry("minecraft:stone", 1))));
        helper.assertTrue(noGold.legendaryRewards().isEmpty() && noGold.rewards().size() == 1, "Non-legendary pool gained a badge");
        for (int count : List.of(-1, 257)) assertBadCount(helper, count, false);
        for (int count : List.of(-1, 257)) assertBadCount(helper, count, true);
        boolean badGrade = false;
        try { new PacketBoxPreview(UUID.randomUUID(), new ItemStack(Items.CHEST), ItemStack.EMPTY, List.of(), List.of(new DisplayReward(new ItemStack(Items.STONE), 1))); }
        catch (IllegalArgumentException expected) { badGrade = true; }
        helper.assertTrue(badGrade, "Non-gold item accepted in legendary list");
        helper.succeed();
    }

    private static void assertBadCount(GameTestHelper helper, int count, boolean legendary) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(UUID.randomUUID()).writeItem(new ItemStack(Items.CHEST)).writeItem(ItemStack.EMPTY);
            if (legendary) buffer.writeVarInt(0);
            buffer.writeVarInt(count);
            boolean rejected = false;
            try { new PacketBoxPreview(buffer); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Invalid preview list count accepted: " + count);
        } finally { buffer.release(); }
    }

    @GameTest(template = "empty")
    public static void legendaryTaczModelDataSurvivesPreviewAndHeartbeatCodec(GameTestHelper helper) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz")) {
            var gun = ItemNBT.parseStrict("tacz:modern_kinetic_gun.withTags{GunId:\"tacz:ak47\",GunCurrentAmmoCount:17}");
            var box = BoxDefinition.create("csgobox:inspect-tacz", "Gold gun", List.of(new RewardEntry(ItemNBT.getStacksData(gun),5)));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BoxService.previewData(UUID.randomUUID(), box).toBytes(buffer);
                helper.assertTrue(ItemStack.matches(gun, new PacketBoxPreview(buffer).legendaryRewards().get(0).stack()), "Legendary inspection lost gun model/NBT");
            } finally { buffer.release(); }
        } else helper.assertTrue(!Boolean.getBoolean("csgobox.taczCompat"), "TaCZ compatibility run missing TaCZ");
        var token = UUID.randomUUID();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new PacketPreviewHeartbeat(token).toBytes(buffer);
            helper.assertTrue(new PacketPreviewHeartbeat(buffer).token().equals(token), "Heartbeat lost its bound preview token");
        } finally { buffer.release(); }
        helper.succeed();
    }
}
