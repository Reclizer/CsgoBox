package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.server.*;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class EditorFeatureGameTests {
    private static FakePlayer player(GameTestHelper helper, boolean operator) {
        return new FakePlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "[BoxFeatureTest]")) {
            @Override public boolean hasPermissions(int level) { return operator || level <= 0; }
        };
    }

    @GameTest(template = "empty")
    public static void testBoxGrantChecksPermissionsMenuRevisionAndReplay(GameTestHelper helper) throws Exception {
        CsgoBoxManage.loadConfigBox();
        String name = "test-grant-" + UUID.randomUUID();
        var box = BoxDefinition.create("csgobox:" + name, name, List.of(new RewardEntry("minecraft:diamond", 5))).withLuckEnabled(false);
        CsgoBoxManage.saveDefinition(box, "");
        String revision = BoxConfigStore.revision(box);
        var player = player(helper, true);
        var denied = player(helper, false);
        denied.containerMenu = new CsgoBoxCraftMenu(7, denied.getInventory(), null);
        helper.assertTrue(!EditorTestBox.give(denied, 7, 1, box.id(), revision).isEmpty(), "Non-op received test items");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, box.id(), revision).isEmpty(), "Missing editor was accepted");
        player.containerMenu = new CsgoBoxCraftMenu(7, player.getInventory(), null);
        helper.assertTrue(!EditorTestBox.give(player, 8, 1, box.id(), revision).isEmpty(), "Wrong menu was accepted");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, box.id(), "stale").isEmpty(), "Stale pool was accepted");
        helper.assertTrue(EditorTestBox.give(player, 7, 1, box.id(), revision).isEmpty(), "Valid test request failed");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, box.id(), revision).isEmpty(), "Replay granted more items");
        helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 1, "Wrong test box count");
        helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGO_KEY0.get()) == 1, "Wrong test key count");
        helper.assertTrue(denied.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 0, "Denied request mutated inventory");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var request = new PacketTestBox(7, 2, box.id(), revision);
            request.toBytes(buffer);
            helper.assertTrue(request.equals(new PacketTestBox(buffer)), "Test request codec changed identity");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledLuckActuallyChangesServerSelection(GameTestHelper helper) {
        var player = player(helper, true);
        player.getAttribute(Attributes.LUCK).setBaseValue(10);
        var box = new BoxDefinition("csgobox:luck-test", "luck-test", "csgobox:csgo_key0", 0, List.of(1,0,0,0,1),
                List.of(), List.of("minecraft:stone"), List.of(), List.of(), List.of(), List.of("minecraft:diamond"));
        for (boolean enabled : List.of(false, true)) {
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get())));
            player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get()));
            var result = BoxOpening.open(player, box.withLuckEnabled(enabled), new Random() {
                @Override public long nextLong(long bound) { return 1; }
            });
            helper.assertTrue(result.success() && result.grade() == (enabled ? 5 : 1), "Server ignored luck toggle");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taczGunDataSurvivesEditorNetworkAndOpening(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("tacz")) {
            helper.assertTrue(!Boolean.getBoolean("csgobox.taczCompat"), "TaCZ compatibility run missing TaCZ");
            helper.succeed(); return;
        }
        String text = "tacz:modern_kinetic_gun.withTags{GunId:\"tacz:ak47\",GunFireMode:\"AUTO\",GunCurrentAmmoCount:17,"
                + "HasBulletInBarrel:1b,GunDisplayId:\"tacz:default\",AttachmentSCOPE:{id:\"tacz:attachment\",Count:1b,"
                + "tag:{AttachmentId:\"tacz:sight_sro_dot\"}},display:{Name:'{\"text\":\"测试 AK\"}'}}";
        var original = ItemNBT.parseStrict(text);
        helper.assertTrue(ItemNBT.getStacks("tacz:modern_kinetic_gun").isEmpty(), "Model-less gun was accepted");
        helper.assertTrue(ItemStack.matches(original, ItemNBT.parseStrict(ItemNBT.getStacksData(original))), "TaCZ NBT roundtrip failed");
        var definition = BoxDefinition.create("csgobox:tacz-test", "tacz-test", List.of(new RewardEntry(ItemNBT.getStacksData(original), 1)));
        var decoded = BoxEditorCodec.decode(BoxEditorCodec.encode(definition));
        var player = player(helper, true);
        player.getInventory().setItem(0, ItemCsgoBox.setBoxInfo(decoded, new ItemStack(ModItems.ITEM_CSGOBOX.get())));
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get()));
        var result = BoxOpening.open(player, decoded, new Random(0));
        helper.assertTrue(result.success() && ItemStack.matches(original, result.reward()), "Opening changed gun data");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new PacketBoxResult(UUID.randomUUID(), result.reward(), 1).toBytes(buffer);
            helper.assertTrue(ItemStack.matches(original, new PacketBoxResult(buffer).reward()), "Result network lost gun data");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void issuedBoxUsesRenamedPoolAndLatestRewards(GameTestHelper helper) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var original = BoxDefinition.create(BoxDefinition.newId(), "Before rename", List.of(new RewardEntry("minecraft:diamond", 1)));
        CsgoBoxManage.saveDefinition(original, "");
        var player = player(helper, true);
        var issued = ItemCsgoBox.setBoxInfo(original, new ItemStack(ModItems.ITEM_CSGOBOX.get(), 2));
        helper.assertTrue(issued.getTag().size() == 1 && issued.getTag().contains(ItemCsgoBox.POOL_ID_TAG), "Box stored more than its pool ID");
        player.getInventory().setItem(0, issued);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get()));
        var renamed = original.withName("After rename").withRewards(List.of(new RewardEntry("minecraft:emerald", 1)));
        CsgoBoxManage.saveDefinition(renamed, BoxConfigStore.revision(original));
        CsgoBoxManage.loadConfigBox();
        helper.assertTrue(issued.getHoverName().getString().equals("After rename"), "Issued box retained the old display name");
        helper.assertTrue(ItemStack.isSameItemSameTags(issued, ItemCsgoBox.setBoxInfo(renamed, new ItemStack(ModItems.ITEM_CSGOBOX.get()))), "Rename prevented stacking");
        var result = BoxOpening.open(player, CsgoBoxManage.find(ItemCsgoBox.boxId(issued)), new Random(0));
        helper.assertTrue(result.success() && result.reward().is(Items.EMERALD), "Issued box did not use the latest server pool");
        var legacy = new ItemStack(ModItems.ITEM_CSGOBOX.get());
        legacy.getOrCreateTag().put("BoxItemInfo", new net.minecraft.nbt.CompoundTag());
        legacy.getTag().getCompound("BoxItemInfo").putString("name", renamed.name());
        player.getInventory().setItem(0, legacy);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get()));
        helper.assertTrue(!BoxOpening.open(player, renamed, new Random(0)).success(), "Old name-only box was accepted");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 1, "Invalid box consumed a key");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void giveCommandAcceptsNamespacedPoolId(GameTestHelper helper) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var box = BoxDefinition.create("csgobox:command/test-" + UUID.randomUUID(), "Command test",
                List.of(new RewardEntry("minecraft:diamond", 1)));
        CsgoBoxManage.saveDefinition(box, "");
        var player = player(helper, true);
        var source = player.createCommandSourceStack().withPermission(2);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        String command = "csgobox give " + box.id();
        helper.assertTrue(dispatcher.execute(command, source) == 1, "Namespaced give command failed");
        helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 1
                && ItemCsgoBox.boxId(player.getInventory().getItem(0)).equals(box.id()), "Command gave the wrong pool");
        helper.assertTrue(dispatcher.execute("csgobox give csgobox:missing-command-pool", source) == 0,
                "Unknown pool was accepted");
        boolean denied = false;
        try { dispatcher.execute(command, source.withPermission(0)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
        helper.assertTrue(denied && player.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 1,
                "Failed or unauthorized command granted extra boxes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catalogRoundTripAndRenameKeepIdsDistinct(GameTestHelper helper) {
        var entries = List.of(new BoxCatalog.Entry("csgobox:first", "Same name"), new BoxCatalog.Entry("csgobox:second", "Same name"));
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new PacketBoxCatalog(entries).toBytes(buffer);
            var decoded = new PacketBoxCatalog(buffer);
            helper.assertTrue(entries.equals(decoded.entries()), "Catalog lost ID/name pairing");
            BoxCatalog.replace(decoded.entries());
            helper.assertTrue(BoxCatalog.entries().size() == 2, "Equal names merged different pools");
            BoxCatalog.replace(List.of(new BoxCatalog.Entry("csgobox:first", "New name")));
            helper.assertTrue(BoxCatalog.displayName("csgobox:first").equals("New name"), "Catalog kept stale name");
            helper.assertTrue(BoxCatalog.displayName("csgobox:second").equals("csgobox:second"), "Removed pool stayed cached");
        } finally { buffer.release(); BoxCatalog.replace(List.of()); }
        helper.succeed();
    }
}
