package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.compat.TaczCompat;
import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.config.RewardEntry;
import com.reclizer.csgobox.item.BoxCatalog;
import com.reclizer.csgobox.packet.PacketBoxRewardNames;
import com.reclizer.csgobox.packet.RewardName;
import com.reclizer.csgobox.server.BoxService;
import com.reclizer.csgobox.utils.ColorTools;
import com.reclizer.csgobox.utils.ItemNBT;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class BoxTooltipGameTests {
    private static final String ID = "csgobox:tooltip-test";

    private static List<Component> tooltip(boolean expanded) {
        var lines = new ArrayList<Component>();
        BoxCatalog.appendRewardTooltip(ID, expanded, lines);
        return lines;
    }

    @GameTest(template = "empty")
    public static void publicSummaryKeepsNamesAndGradesButExcludesGold(GameTestHelper helper) {
        var pool = BoxDefinition.create(ID, "Tooltip test", List.of(
                new RewardEntry("minecraft:diamond.withCount(3).withTags{private_data:\"not-a-name\"}", 1),
                new RewardEntry("minecraft:emerald", 2),
                new RewardEntry("minecraft:iron_ingot", 3),
                new RewardEntry("minecraft:apple", 4),
                new RewardEntry("minecraft:netherite_sword.withTags{display:{Name:'{\"text\":\"Secret gold\"}'}}", 5)));
        var packet = BoxService.rewardNames(pool);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.toBytes(buffer);
            String wire = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
            helper.assertTrue(!wire.contains("Secret gold") && !wire.contains("netherite_sword")
                    && !wire.contains("private_data") && !wire.contains("not-a-name"), "Private reward data leaked");
            var decoded = new PacketBoxRewardNames(buffer);
            helper.assertTrue(decoded.equals(packet) && decoded.rewards().size() == 4, "Summary codec lost public entries");
            helper.assertTrue(decoded.rewards().get(0).name().getContents() instanceof TranslatableContents,
                    "Server resolved the client-language item name too early");
            BoxCatalog.replace(List.of(new BoxCatalog.Entry(ID, "Tooltip test")));
            BoxCatalog.replaceRewardNames(ID, decoded.rewards());
            var collapsed = tooltip(false);
            var count = (TranslatableContents) collapsed.get(0).getContents();
            helper.assertTrue(collapsed.size() == 2 && count.getArgs()[0].equals(4), "Collapsed count included gold");
            var expanded = tooltip(true);
            helper.assertTrue(expanded.size() == 4, "Expanded list added details beyond names");
            for (int i = 0; i < 4; i++) {
                helper.assertTrue(expanded.get(i).getString().equals(decoded.rewards().get(i).name().getString()), "Name changed");
                helper.assertTrue(expanded.get(i).getStyle().getColor().getValue() == (ColorTools.colorItems(i + 1) & 0xFFFFFF),
                        "Wrong rarity colour");
            }
        } finally { buffer.release(); BoxCatalog.replace(List.of()); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tooltipRefreshesNamesAndClearsRemovedPools(GameTestHelper helper) {
        try {
            var entry = new BoxCatalog.Entry(ID, "Tooltip test");
            BoxCatalog.replace(List.of(entry));
            Component decorated = Component.literal("First").withStyle(s -> s.withColor(0x00FF00))
                    .append(Component.literal("\u00a7c\nSecond").withStyle(s -> s.withColor(0xFF0000).withBold(true)));
            BoxCatalog.replaceRewardNames(ID, List.of(new RewardName(decorated, 2)));
            var name = tooltip(true).get(0);
            helper.assertTrue(name.getString().equals("First Second") && name.getSiblings().isEmpty()
                    && name.getStyle().getColor().getValue() == 0x8D5EFF && !name.getStyle().isBold(),
                    "Custom formatting overrode the rarity colour or added lines");
            BoxCatalog.replaceRewardNames(ID, List.of(new RewardName(Component.literal("Replacement"), 4)));
            helper.assertTrue(tooltip(true).get(0).getString().equals("Replacement"), "Summary kept stale formatted names");
            BoxCatalog.replace(List.of(entry));
            helper.assertTrue(((TranslatableContents) tooltip(true).get(0).getContents()).getKey()
                    .equals("tooltips.csgobox.rewards_unavailable"), "Catalog reload retained old rewards");
            var goldOnly = BoxDefinition.create(ID, "Gold", List.of(new RewardEntry("minecraft:diamond", 5)));
            BoxCatalog.replaceRewardNames(ID, BoxService.rewardNames(goldOnly).rewards());
            helper.assertTrue(((TranslatableContents) tooltip(true).get(0).getContents()).getKey()
                    .equals("tooltips.csgobox.rewards_empty"), "Gold-only pool exposed an item");
            BoxCatalog.replace(List.of());
            BoxCatalog.replaceRewardNames(ID, List.of(new RewardName(Component.literal("Late packet"), 1)));
            helper.assertTrue(((TranslatableContents) tooltip(true).get(0).getContents()).getKey()
                    .equals("tooltips.csgobox.rewards_unavailable"), "Removed pool retained or accepted a late summary");
        } finally { BoxCatalog.replace(List.of()); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summaryCodecRejectsInvalidCountsAndHiddenGrades(GameTestHelper helper) {
        for (int count : List.of(-1, 257)) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeUtf(ID, 128).writeVarInt(count);
                boolean rejected = false;
                try { new PacketBoxRewardNames(buffer); }
                catch (IllegalArgumentException expected) { rejected = true; }
                helper.assertTrue(rejected, "Invalid entry count accepted: " + count);
            } finally { buffer.release(); }
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf(ID, 128).writeVarInt(1).writeComponent(Component.literal("Gold")).writeVarInt(5);
            boolean rejected = false;
            try { new PacketBoxRewardNames(buffer); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Hidden grade accepted by public summary codec");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taczNamesUseServerPackMetadataAndRespectCustomNames(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("tacz")) {
            helper.assertTrue(!Boolean.getBoolean("csgobox.taczCompat"), "TaCZ compatibility run missing TaCZ");
            helper.succeed(); return;
        }
        var gun = ItemNBT.parseStrict("tacz:modern_kinetic_gun.withTags{GunId:\"tacz:ak47\"}");
        var name = TaczCompat.rewardName(gun);
        helper.assertTrue(name.getContents() instanceof TranslatableContents contents
                && contents.getKey().contains("ak47"), "Dedicated server did not resolve the gun's pack name");
        gun.setHoverName(Component.literal("Custom AK"));
        helper.assertTrue(TaczCompat.rewardName(gun).getString().equals("Custom AK"), "Custom gun name was lost");
        helper.succeed();
    }
}
