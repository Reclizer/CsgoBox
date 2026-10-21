package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.config.RewardEntry;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.server.BoxOpening;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.Random;

@GameTestHolder(CsgoBox.MODID)
@PrefixGameTestTemplate(false)
public final class BoxGameTests {
    private static BoxDefinition box() {
        return BoxDefinition.create("test", List.of(new RewardEntry("minecraft:diamond.withCount(3)", 5)));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[CsgoBoxTest]"));
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setHealth(20);
        return player;
    }

    private static void heldBox(ServerPlayer player, int count) {
        ItemStack stack = ItemCsgoBox.setBoxInfo(box(), new ItemStack(ModItems.ITEM_CSGOBOX.get(), count));
        player.getInventory().setItem(0, stack);
    }

    @GameTest(template = "empty")
    public static void defaultConfigurationAndReloadCommandWork(GameTestHelper helper) throws java.io.IOException {
        int count = com.reclizer.csgobox.config.CsgoBoxManage.loadConfigBox();
        helper.assertTrue(count > 0, "Default server configuration did not load");
        var server = helper.getLevel().getServer();
        int reloaded = server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "csgobox reload");
        helper.assertTrue(reloaded == count, "Admin reload command failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nbtAndCountsRoundTripWithoutCraftTweaker(GameTestHelper helper) {
        String text = "minecraft:diamond.withTags{display:{Name:'{\"text\":\".withCount(2)\"}'},Enchantments:[{id:\"minecraft:fortune\",lvl:3s}]}.withCount(3)";
        ItemStack item = ItemNBT.parseStrict(text);
        helper.assertTrue(item.getCount() == 3, "Count was lost");
        ItemStack roundTrip = ItemNBT.parseStrict(ItemNBT.getStacksData(item));
        helper.assertTrue(ItemStack.matches(item, roundTrip), "NBT/count round trip changed the reward");
        helper.assertTrue(ItemNBT.parseStrict("minecraft:diamond.withCount(2).withTags{test:1}").getTag().getInt("test") == 1,
                "Count-before-tags format failed");
        for (String invalid : List.of("missing:item", "minecraft:air", "minecraft:diamond.withCount(0)",
                "minecraft:diamond.withCount(65)", "minecraft:diamond.withTags{", "Invalid ID"))
            helper.assertTrue(ItemNBT.getStacks(invalid).isEmpty(), "Invalid item was accepted: " + invalid);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyOneKeyIsConsumedAndGoldOnlyBoxWorks(GameTestHelper helper) {
        var player = player(helper);
        heldBox(player, 2);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 4));
        player.getInventory().setItem(2, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 6));
        var result = BoxOpening.open(player, box(), new Random(0));
        helper.assertTrue(result.success() && result.grade() == 5, "Gold-only pool did not open");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Wrong number of boxes consumed");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 3 && player.getInventory().getItem(2).getCount() == 6,
                "Opening consumed more than one key");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3, "Reward count was not delivered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingKeyDoesNotConsumeBox(GameTestHelper helper) {
        var player = player(helper);
        heldBox(player, 2);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY1.get(), 8));
        var result = BoxOpening.open(player, box(), new Random(0));
        helper.assertTrue(!result.success() && player.getMainHandItem().getCount() == 2, "Missing key consumed box");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 8, "Wrong key was consumed");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 0, "Reward delivered without key");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgedLegacyRewardIsIgnoredAndNotShared(GameTestHelper helper) {
        var player = player(helper);
        heldBox(player, 2);
        CompoundTag legacy = player.getMainHandItem().getOrCreateTag().getCompound(ItemCsgoBox.BOX_INFO_TAG);
        legacy.putString("key", "minecraft:dirt");
        legacy.putIntArray("random", new int[]{1,0,0,0,0});
        legacy.putString("grade5", "minecraft:netherite_block");
        var share = player.getMainHandItem().getItem().getShareTag(player.getMainHandItem()).getCompound(ItemCsgoBox.BOX_INFO_TAG);
        helper.assertTrue(share.size() == 1 && share.getString("name").equals("test"), "Private config leaked through share tag");
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 2));
        var result = BoxOpening.open(player, box(), new Random(0));
        helper.assertTrue(result.reward().is(Items.DIAMOND), "Client/legacy NBT selected the reward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryMergesExistingStack(GameTestHelper helper) {
        var player = player(helper);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        heldBox(player, 2);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 2));
        player.getInventory().setItem(2, new ItemStack(Items.DIAMOND, 60));
        helper.assertTrue(BoxOpening.open(player, box(), new Random(0)).success(), "Opening failed");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 63, "Existing stack was not filled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryDropsOnlyTheRemainder(GameTestHelper helper) {
        var player = player(helper);
        player.getAbilities().instabuild = true;
        var position = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 1, 2));
        player.setPos(position);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        heldBox(player, 2);
        player.getInventory().setItem(1, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 2));
        player.getInventory().setItem(2, new ItemStack(Items.DIAMOND, 63));
        helper.assertTrue(BoxOpening.open(player, box(), new Random(0)).success(), "Opening failed");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 64, "Available stack space was not used");
        int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        player.getBoundingBox().inflate(3)).stream()
                .filter(entity -> entity.getItem().is(Items.DIAMOND)).mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(dropped == 2, "Expected two remaining diamonds on the ground, got " + dropped);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void editorKeepsItemsAndGradesTogether(GameTestHelper helper) {
        var player = player(helper);
        var menu = new CsgoBoxCraftMenu(7, player.getInventory(), null);
        ItemStack taggedAir = new ItemStack(Items.AIR);
        taggedAir.setTag(new CompoundTag());
        menu.getSlot(1).set(taggedAir);
        int[] slots = {0, 6, 7, 11, 12, 14, 15, 16, 17, 34};
        int[] grades = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int i : slots) menu.getSlot(i).set(new ItemStack(Items.DIAMOND, 3));
        var rewards = menu.rewards();
        helper.assertTrue(rewards.size() == slots.length, "Sparse editor slots lost or added entries");
        for (int i = 0; i < slots.length; i++) {
            helper.assertTrue(rewards.get(i).grade() == grades[i], "Grade boundary incorrect at slot " + slots[i]);
            helper.assertTrue(ItemNBT.parseStrict(rewards.get(i).item()).getCount() == 3, "Editor lost stack count");
        }
        menu.quickMoveStack(player, 34);
        helper.assertTrue(menu.getSlot(34).getItem().isEmpty(), "Last reward slot did not shift-click into player inventory");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3, "Shift-click lost items");
        menu.removed(player);
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 30, "Closing editor lost items");
        helper.succeed();
    }
}
