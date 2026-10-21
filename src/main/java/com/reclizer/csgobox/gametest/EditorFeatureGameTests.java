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
        var box = BoxDefinition.create(name, List.of(new RewardEntry("minecraft:diamond", 5))).withLuckEnabled(false);
        CsgoBoxManage.saveDefinition(box, "");
        String revision = BoxConfigStore.revision(box);
        var player = player(helper, true);
        var denied = player(helper, false);
        denied.containerMenu = new CsgoBoxCraftMenu(7, denied.getInventory(), null);
        helper.assertTrue(!EditorTestBox.give(denied, 7, 1, name, revision).isEmpty(), "Non-op received test items");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, name, revision).isEmpty(), "Missing editor was accepted");
        player.containerMenu = new CsgoBoxCraftMenu(7, player.getInventory(), null);
        helper.assertTrue(!EditorTestBox.give(player, 8, 1, name, revision).isEmpty(), "Wrong menu was accepted");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, name, "stale").isEmpty(), "Stale pool was accepted");
        helper.assertTrue(EditorTestBox.give(player, 7, 1, name, revision).isEmpty(), "Valid test request failed");
        helper.assertTrue(!EditorTestBox.give(player, 7, 1, name, revision).isEmpty(), "Replay granted more items");
        helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 1, "Wrong test box count");
        helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGO_KEY0.get()) == 1, "Wrong test key count");
        helper.assertTrue(denied.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 0, "Denied request mutated inventory");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var request = new PacketTestBox(7, 2, name, revision);
            request.toBytes(buffer);
            helper.assertTrue(request.equals(new PacketTestBox(buffer)), "Test request codec changed identity");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledLuckActuallyChangesServerSelection(GameTestHelper helper) {
        var player = player(helper, true);
        player.getAttribute(Attributes.LUCK).setBaseValue(10);
        var box = new BoxDefinition("luck-test", "csgobox:csgo_key0", 0, List.of(1,0,0,0,1),
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
        var definition = BoxDefinition.create("tacz-test", List.of(new RewardEntry(ItemNBT.getStacksData(original), 1)));
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
}
