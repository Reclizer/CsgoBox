package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.server.PlacedBoxOpening;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class PlacedBoxGameTests {
    private static BoxDefinition definition(String key) {
        return new BoxDefinition(BoxDefinition.newId(), "Ground box", key, 0, List.of(0,0,0,0,1), List.of(),
                List.of("minecraft:diamond.withTags{groundReward:1b}.withCount(3)"), null, null, null, null);
    }
    private static BlockPos pos(GameTestHelper helper) { return helper.absolutePos(new BlockPos(2,1,2)); }
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "[GroundBoxTest]"));
        player.setPos(Vec3.atCenterOf(pos(helper)).add(0,0,2));
        player.getInventory().selected = 0;
        player.getAbilities().instabuild = false; player.getAbilities().mayBuild = true;
        return player;
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void withConfig(BoxDefinition box, Checked test) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var file = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json");
        byte[] before = java.nio.file.Files.readAllBytes(file);
        try { CsgoBoxManage.saveDefinition(box, ""); test.run(); }
        finally { java.nio.file.Files.write(file, before); CsgoBoxManage.loadConfigBox(); }
    }
    private static void ground(GameTestHelper helper) {
        helper.getLevel().setBlockAndUpdate(pos(helper).below(), Blocks.STONE.defaultBlockState());
    }
    private static UseOnContext placement(GameTestHelper helper, FakePlayer player) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atLowerCornerOf(pos(helper)).add(.5,0,.5), Direction.UP, pos(helper).below(), false));
    }
    private static PlacedBoxEntity place(GameTestHelper helper, BoxDefinition box) {
        ground(helper);
        helper.getLevel().setBlockAndUpdate(pos(helper), ModBlocks.BOX.get().defaultBlockState());
        var entity = (PlacedBoxEntity) helper.getLevel().getBlockEntity(pos(helper));
        entity.setPoolId(box.id());
        return entity;
    }
    private static List<ItemStack> drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos(helper)).inflate(2)).stream().map(ItemEntity::getItem).toList();
    }

    static void complete(GameTestHelper helper,PlacedBoxEntity box) {
        var saved=box.saveWithoutMetadata();
        saved.putLong("OpeningStarted",helper.getLevel().getGameTime()-com.reclizer.csgobox.utils.GroundAnimation.TOTAL_TICKS);
        box.load(saved);
        PlacedBoxEntity.serverTick(helper.getLevel(),box.getBlockPos(),box.getBlockState(),box);
    }

    @GameTest(template = "empty")
    public static void shiftPlacementConsumesOneAndTransfersOnlyPoolIdentity(GameTestHelper helper) throws Exception {
        var box = definition("csgobox:csgo_key0");
        withConfig(box, () -> {
            ground(helper); var player = player(helper);
            ItemStack stack = ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get(), 3));
            stack.getOrCreateTagElement("BlockEntityTag").putString(ItemCsgoBox.POOL_ID_TAG, "csgobox:forged");
            stack.getTagElement("BlockEntityTag").putString("reward", "minecraft:netherite_block");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(stack.useOn(placement(helper, player)) == InteractionResult.PASS, "Plain use unexpectedly placed a block");
            player.setShiftKeyDown(true); player.setYRot(90);
            helper.assertTrue(stack.useOn(placement(helper, player)).consumesAction(), "Shift placement failed");
            var state = helper.getLevel().getBlockState(pos(helper));
            var entity = (PlacedBoxEntity) helper.getLevel().getBlockEntity(pos(helper));
            helper.assertTrue(state.is(ModBlocks.BOX.get()) && state.getValue(PlacedBoxBlock.FACING) == player.getDirection().getOpposite(), "Placement facing is wrong");
            helper.assertTrue(stack.getCount() == 2 && entity.poolId().equals(box.id()), "Placement lost identity or consumed wrong count");
            helper.assertTrue(entity.getUpdateTag().size() == 1 && !entity.saveWithFullMetadata().contains("reward"), "Placement copied arbitrary embedded data");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledPlacementRollsBackBoxAndInventory(GameTestHelper helper) throws Exception {
        var box = definition("");
        withConfig(box, () -> {
            ground(helper); var player = player(helper); player.setShiftKeyDown(true);
            var stack = ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get(), 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> { if (event.getEntity() == player) event.setCanceled(true); };
            MinecraftForge.EVENT_BUS.addListener(cancel);
            try { stack.useOn(placement(helper, player)); }
            finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            helper.assertTrue(helper.getLevel().getBlockState(pos(helper)).isAir() && stack.getCount() == 2,
                    "Canceled Forge placement left a box or consumed an item");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void persistencePickAndBreakingPreservePoolWithoutRewards(GameTestHelper helper) throws Exception {
        var box = definition("");
        withConfig(box, () -> {
            var entity = place(helper, box);
            var saved = entity.saveWithFullMetadata();
            var loaded = (PlacedBoxEntity) BlockEntity.loadStatic(pos(helper), entity.getBlockState(), saved);
            helper.assertTrue(loaded != null && loaded.poolId().equals(box.id()), "World save/load lost pool ID");
            helper.assertTrue(ItemCsgoBox.boxId(ModBlocks.BOX.get().getCloneItemStack(helper.getLevel(), pos(helper), entity.getBlockState())).equals(box.id()),
                    "Pick block lost pool ID");
            helper.getLevel().destroyBlock(pos(helper), true);
            var boxes = drops(helper).stream().filter(s -> ItemCsgoBox.boxId(s).equals(box.id())).toList();
            helper.assertTrue(boxes.size() == 1 && boxes.get(0).getCount() == 1 && boxes.get(0).getTag().size() == 1, "Breaking lost or duplicated the unopened box");
            helper.assertTrue(drops(helper).stream().noneMatch(s -> s.is(Items.DIAMOND)), "Breaking awarded a reward without opening");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void removingSupportReturnsOneUnopenedBox(GameTestHelper helper) throws Exception {
        var box = definition("");
        withConfig(box, () -> {
            place(helper, box);
            helper.getLevel().destroyBlock(pos(helper).below(), false);
            helper.assertTrue(helper.getLevel().getBlockState(pos(helper)).isAir(), "Unsupported box remained floating");
            helper.assertTrue(drops(helper).stream().filter(s -> ItemCsgoBox.boxId(s).equals(box.id())).mapToInt(ItemStack::getCount).sum() == 1,
                    "Removing support lost or duplicated the box");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heldKeyNbtIsRequiredAndOnlyFirstPlayerCanClaim(GameTestHelper helper) throws Exception {
        var box = definition("minecraft:tripwire_hook.withTags{groundKey:1b}");
        withConfig(box, () -> {
            place(helper, box); var first = player(helper); var second = player(helper);
            var keys = ItemNBT.parseStrict(box.key()); keys.setCount(3);
            first.getInventory().setItem(1, keys);
            helper.assertTrue(!PlacedBoxOpening.open(first, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "A backpack-only key was accepted");
            first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TRIPWIRE_HOOK, 2));
            helper.assertTrue(!PlacedBoxOpening.open(first, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "Wrong key NBT was accepted");
            first.setItemInHand(InteractionHand.MAIN_HAND, keys.copy());
            second.setItemInHand(InteractionHand.MAIN_HAND, keys.copy());
            var result = PlacedBoxOpening.open(first, pos(helper), InteractionHand.MAIN_HAND, new Random(0));
            helper.assertTrue(result.success() && first.getMainHandItem().getCount() == 2 && first.getInventory().getItem(1).getCount() == 3, "Opening consumed wrong key/amount");
            helper.assertTrue(first.getInventory().countItem(Items.DIAMOND)==0 && drops(helper).isEmpty(),"Reward delivered before animation");
            var opening=(PlacedBoxEntity)helper.getLevel().getBlockEntity(pos(helper));
            helper.assertTrue(opening.isOpening(),"Placed case did not start animation");
            helper.assertTrue(!PlacedBoxOpening.open(second, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success()
                    && !PlacedBoxOpening.open(first, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "Duplicate request awarded again");
            helper.assertTrue(second.getMainHandItem().getCount() == 3 && second.getInventory().countItem(Items.DIAMOND) == 0, "Losing player consumed key or received reward");
            helper.assertTrue(drops(helper).stream().noneMatch(s -> ItemCsgoBox.boxId(s).equals(box.id())), "Opening also dropped an unopened box");
            complete(helper,opening);
            helper.assertTrue(opening.isEmpty() && drops(helper).stream().filter(s->s.is(Items.DIAMOND)
                    && s.getTag().getBoolean("groundReward")).mapToInt(ItemStack::getCount).sum()==3,"Delayed drop lost NBT or quantity");
            PlacedBoxOpening.open(second,pos(helper),InteractionHand.MAIN_HAND,new Random());
            helper.assertTrue(second.getMainHandItem().getCount()==3 && drops(helper).stream().filter(s->s.is(Items.DIAMOND))
                    .mapToInt(ItemStack::getCount).sum()==3,"Empty-case interaction consumed a key or redelivered");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandKeyAndFullInventoryDropOnlyAfterAnimation(GameTestHelper helper) throws Exception {
        var box = definition("csgobox:csgo_key0");
        withConfig(box, () -> {
            place(helper, box); var player = player(helper);
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 2));
            helper.assertTrue(PlacedBoxOpening.open(player, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "Offhand key could not open box");
            helper.assertTrue(player.getOffhandItem().getCount() == 1 && player.getMainHandItem().getCount() == 64, "Wrong hand was consumed");
            helper.assertTrue(drops(helper).isEmpty(),"Prize dropped before animation completed");
            complete(helper,(PlacedBoxEntity)helper.getLevel().getBlockEntity(pos(helper)));
            helper.assertTrue(drops(helper).stream().filter(s -> s.is(Items.DIAMOND) && s.hasTag() && s.getTag().getBoolean("groundReward"))
                    .mapToInt(ItemStack::getCount).sum() == 3, "Full inventory did not drop exactly the reward overflow");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noKeyBoxUsesLatestPoolAndDeniedInteractionDoesNotConsume(GameTestHelper helper) throws Exception {
        var original = definition("csgobox:csgo_key0");
        withConfig(original, () -> {
            place(helper, original); var player = player(helper);
            var updated = new BoxDefinition(original.id(), "Renamed", "", 0, List.of(0,0,0,0,1), List.of(),
                    List.of("minecraft:emerald.withCount(2)"), null, null, null, null);
            CsgoBoxManage.saveDefinition(updated, BoxConfigStore.revision(original));
            CsgoBoxManage.loadConfigBox();
            player.setPos(Vec3.atCenterOf(pos(helper)).add(30,0,0));
            helper.assertTrue(!PlacedBoxOpening.open(player, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "Out-of-reach opening succeeded");
            player.setPos(Vec3.atCenterOf(pos(helper)).add(0,0,2));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos(helper)), Direction.UP, pos(helper), false);
            Consumer<PlayerInteractEvent.RightClickBlock> cancel = event -> { if (event.getEntity() == player) event.setCanceled(true); };
            MinecraftForge.EVENT_BUS.addListener(cancel);
            try { player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit); }
            finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            helper.assertTrue(helper.getLevel().getBlockState(pos(helper)).is(ModBlocks.BOX.get()) && player.getInventory().countItem(Items.EMERALD) == 0,
                    "Canceled interaction still opened the box");
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            var opening=(PlacedBoxEntity)helper.getLevel().getBlockEntity(pos(helper));
            helper.assertTrue(opening.isOpening() && drops(helper).isEmpty(),"Empty-hand interaction did not start the current animation");
            complete(helper,opening);
            helper.assertTrue(opening.isEmpty() && drops(helper).stream().filter(s->s.is(Items.EMERALD))
                    .mapToInt(ItemStack::getCount).sum()==2,"Delayed opening did not use latest configured reward");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingPoolOrSpectatorCannotOpenAndFailedPlacementKeepsItem(GameTestHelper helper) throws Exception {
        var box = definition("csgobox:csgo_key0");
        withConfig(box, () -> {
            var entity = place(helper, box); var player = player(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ITEM_CSGO_KEY0.get(), 2));
            entity.setPoolId("csgobox:missing-ground-test");
            helper.assertTrue(!PlacedBoxOpening.open(player, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success()
                    && player.getMainHandItem().getCount() == 2, "Missing pool consumed key");
            entity.setPoolId(box.id());
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            helper.assertTrue(!PlacedBoxOpening.open(player, pos(helper), InteractionHand.MAIN_HAND, new Random(0)).success(), "Spectator opened box");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            helper.getLevel().removeBlock(pos(helper), false);
            helper.getLevel().setBlockAndUpdate(pos(helper).below(), Blocks.OAK_SLAB.defaultBlockState());
            var stack = ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get(), 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack); player.setShiftKeyDown(true);
            helper.assertTrue(!stack.useOn(placement(helper, player)).consumesAction() && stack.getCount() == 2,
                    "Unsupported placement consumed an item");
        }); helper.succeed();
    }
}
