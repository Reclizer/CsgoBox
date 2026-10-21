package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.server.*;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("csgobox")
@PrefixGameTestTemplate(false)
public final class AcquisitionGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "[AcquisitionTest]")) {
            @Override public boolean hasPermissions(int level) { return true; }
        };
    }
    private static BoxDefinition box(String key, AcquisitionDefinition boxRules, AcquisitionDefinition keyRules) {
        return new BoxDefinition(BoxDefinition.newId(), "Acquisition test", key, 0, List.of(0,0,0,0,1), List.of(),
                List.of("minecraft:diamond"), null, null, null, null, true, null, boxRules, keyRules);
    }
    private static BoxDefinition disabled(BoxDefinition box) {
        return new BoxDefinition(box.id(), box.name(), box.key(), 0, box.random(), List.of(), box.grade1(), box.grade2(),
                box.grade3(), box.grade4(), box.grade5(), box.luckEnabled(), box.displayOrder(),
                AcquisitionDefinition.disabled(), AcquisitionDefinition.disabled());
    }
    private static void held(FakePlayer player, BoxDefinition box) {
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get(), 2)));
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void withConfig(GameTestHelper helper, BoxDefinition box, Checked test) throws Exception {
        CsgoBoxManage.loadConfigBox();
        var path = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csbox/box.json");
        byte[] before = java.nio.file.Files.readAllBytes(path);
        try {
            CsgoBoxManage.saveDefinition(box, "");
            ConfiguredAcquisition.refresh(helper.getLevel().getServer());
            test.run();
        } finally {
            java.nio.file.Files.write(path, before);
            CsgoBoxManage.loadConfigBox();
            ConfiguredAcquisition.refresh(helper.getLevel().getServer());
        }
    }

    @GameTest(template = "empty")
    public static void hiddenRewardsRemainDrawableAndNoKeyIsRequired(GameTestHelper helper) {
        var entries = new ArrayList<RewardEntry>();
        for (int i = 0; i < 20; i++) entries.add(new RewardEntry("minecraft:stone", 1));
        entries.add(new RewardEntry("minecraft:emerald.withCount(3)", 1));
        entries.add(new RewardEntry("minecraft:diamond", 5));
        var box = box("", null, null).withRewards(entries);
        helper.assertTrue(box.previewRewards().size() == 20 && box.previewRewards().stream()
                .noneMatch(e -> e.item().contains("emerald")), "Preview did not hide entry 21");
        var player = player(helper); held(player, box);
        player.getInventory().setItem(1, new ItemStack(Items.TRIPWIRE_HOOK, 4));
        var result = BoxOpening.open(player, box, new Random(0) {
            @Override public int nextInt(int bound) { return bound - 1; }
        });
        helper.assertTrue(result.success() && result.reward().is(Items.EMERALD), "Hidden reward could not be drawn");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 3 && player.getMainHandItem().getCount() == 1,
                "Optional-key opening delivered or consumed incorrectly");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 4, "Key-free box consumed an unrelated item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sampledKeyRequiresItsNbtAndConsumesOnlyOne(GameTestHelper helper) {
        var key = ItemNBT.parseStrict("minecraft:tripwire_hook.withTags{keyPool:\"alpha\"}");
        var box = box(ItemNBT.getStacksData(key), null, null);
        var player = player(helper); held(player, box);
        player.getInventory().setItem(1, new ItemStack(Items.TRIPWIRE_HOOK, 4));
        helper.assertTrue(!BoxOpening.open(player, box, new Random(0)).success(), "Plain hook matched a custom key");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "Invalid key consumed the box");
        key.setCount(3); player.getInventory().setItem(2, key);
        helper.assertTrue(BoxOpening.open(player, box, new Random(0)).success(), "Exact key NBT was rejected");
        helper.assertTrue(key.getCount() == 2 && player.getInventory().getItem(1).getCount() == 4, "Wrong key consumption");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noKeyTestGrantGivesOnlyBox(GameTestHelper helper) throws Exception {
        var box = box("", null, null);
        withConfig(helper, box, () -> {
            var player = player(helper);
            player.containerMenu = new CsgoBoxCraftMenu(7, player.getInventory(), null);
            helper.assertTrue(EditorTestBox.give(player, 7, 1, box.id(), BoxConfigStore.revision(box)).isEmpty(), "No-key test grant failed");
            helper.assertTrue(player.getInventory().countItem(ModItems.ITEM_CSGOBOX.get()) == 1
                    && player.getInventory().items.stream().filter(s -> !s.isEmpty()).count() == 1, "Unexpected key was granted");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesMatchNbtConsumeMaterialsAndRefreshOpenCrafting(GameTestHelper helper) throws Exception {
        var grid = new ArrayList<>(Collections.nCopies(9, ""));
        grid.set(4, "minecraft:diamond.withTags{recipeTest:1b}");
        var rules = new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true, grid, 2), null, null);
        var box = box("minecraft:tripwire_hook", rules,
                new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true,
                        List.of("minecraft:flint", "", "", "", "", "", "", "", ""), 3), null, null));
        withConfig(helper, box, () -> {
            var server = helper.getLevel().getServer();
            var recipe = ConfiguredAcquisition.recipe(box, false);
            helper.assertTrue(recipe.getWidth() == 1 && recipe.getHeight() == 1, "Recipe whitespace was not trimmed");
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(List.of(recipe)).write(buffer);
                var decoded = new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(buffer);
                helper.assertTrue(decoded.getRecipes().size() == 1, "Recipe network roundtrip failed");
            } finally { buffer.release(); }
            var player = player(helper);
            var menu = new CraftingMenu(4, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
            player.containerMenu = menu;
            menu.getSlot(1).set(new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Recipe ignored ingredient NBT");
            var ingredient = ItemNBT.parseStrict(grid.get(4)); ingredient.setCount(3);
            menu.getSlot(1).set(ingredient);
            helper.assertTrue(ItemCsgoBox.boxId(menu.getSlot(0).getItem()).equals(box.id())
                    && menu.getSlot(0).getItem().getCount() == 2, "Recipe output lost pool ID/count");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().getCount() == 2 && menu.getSlot(1).getItem().getCount() == 2,
                    "Crafting did not consume exactly one ingredient");
            helper.assertTrue(server.getRecipeManager().byKey(ConfiguredAcquisition.recipeId(box, true)).isPresent(), "Separate key recipe missing");
            var keyRecipe = ConfiguredAcquisition.recipe(box, true);
            helper.assertTrue(keyRecipe.getResultItem(server.registryAccess()).is(Items.TRIPWIRE_HOOK)
                    && keyRecipe.getResultItem(server.registryAccess()).getCount() == 3, "Key recipe has wrong output");
            CsgoBoxManage.saveDefinition(disabled(box), BoxConfigStore.revision(box));
            ConfiguredAcquisition.refresh(server);
            ConfiguredAcquisition.refreshCrafting(player);
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Disabled recipe left a claimable cached result");
            helper.assertTrue(server.getRecipeManager().byKey(ConfiguredAcquisition.recipeId(box, false)).isEmpty()
                    && server.getRecipeManager().byKey(ConfiguredAcquisition.recipeId(box, true)).isEmpty(), "Disabled recipes remained registered");
            helper.assertTrue(server.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("minecraft:crafting_table")).isPresent(), "Refresh removed a vanilla recipe");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boxAndKeyDropsAndTradesAreIndependentAndReloadDoesNotDuplicate(GameTestHelper helper) throws Exception {
        var boxRules = new AcquisitionDefinition(null, new AcquisitionDefinition.Drop(true, 1, List.of("minecraft:zombie"), 2),
                new AcquisitionDefinition.Trade(true, "minecraft:cleric", 2, "minecraft:emerald.withCount(4)", "minecraft:diamond", 2, 8, 5));
        var keyRules = new AcquisitionDefinition(null, new AcquisitionDefinition.Drop(true, 1, List.of("minecraft:skeleton"), 3),
                new AcquisitionDefinition.Trade(true, "minecraft:weaponsmith", 1, "minecraft:iron_ingot.withCount(2)", "", 1, 12, 1));
        var box = box("minecraft:tripwire_hook.withTags{testKey:1b}", boxRules, keyRules);
        withConfig(helper, box, () -> {
            var server = helper.getLevel().getServer();
            var zombie = new net.minecraft.world.entity.monster.Zombie(helper.getLevel());
            var event = new LivingDropsEvent(zombie, zombie.damageSources().generic(), new ArrayList<>(), 0, true);
            ConfiguredAcquisition.drops(event);
            helper.assertTrue(event.getDrops().stream().anyMatch(e -> ItemCsgoBox.boxId(e.getItem()).equals(box.id()) && e.getItem().getCount() == 2), "Box drop missing");
            helper.assertTrue(event.getDrops().stream().noneMatch(e -> e.getItem().is(Items.TRIPWIRE_HOOK)), "Box rule triggered key drop");
            var skeleton = new net.minecraft.world.entity.monster.Skeleton(net.minecraft.world.entity.EntityType.SKELETON, helper.getLevel());
            var keyEvent = new LivingDropsEvent(skeleton, skeleton.damageSources().generic(), new ArrayList<>(), 0, true);
            ConfiguredAcquisition.drops(keyEvent);
            helper.assertTrue(keyEvent.getDrops().stream().anyMatch(e -> e.getItem().is(Items.TRIPWIRE_HOOK)
                    && e.getItem().getCount() == 3 && e.getItem().getTag().getBoolean("testKey")), "Key drop lost NBT/count");
            var offer = ConfiguredAcquisition.offer(box, false);
            var payment = new ItemStack(Items.EMERALD, 4); var second = new ItemStack(Items.DIAMOND);
            helper.assertTrue(offer.take(payment, second) && payment.isEmpty() && second.isEmpty(), "Trade did not take both costs");
            helper.assertTrue(offer.getMaxUses() == 8 && offer.getXp() == 5 && offer.assemble().getCount() == 2
                    && ItemCsgoBox.boxId(offer.assemble()).equals(box.id()), "Trade output/settings changed");
            int before = VillagerTrades.TRADES.get(VillagerProfession.CLERIC).get(2).length;
            ConfiguredAcquisition.refresh(server); ConfiguredAcquisition.refresh(server);
            helper.assertTrue(VillagerTrades.TRADES.get(VillagerProfession.CLERIC).get(2).length == before, "Reload duplicated trade entries");
            helper.assertTrue(Arrays.stream(VillagerTrades.TRADES.get(VillagerProfession.CLERIC).get(2))
                    .map(f -> f.getOffer(zombie, zombie.getRandom())).filter(Objects::nonNull)
                    .anyMatch(o -> ItemCsgoBox.boxId(o.getResult()).equals(box.id())), "Trade factory not registered for profession/level");
            CsgoBoxManage.saveDefinition(disabled(box), BoxConfigStore.revision(box)); ConfiguredAcquisition.refresh(server);
            helper.assertTrue(VillagerTrades.TRADES.get(VillagerProfession.CLERIC).get(2).length == before - 1, "Disabled trade remained registered");
            var disabledEvent = new LivingDropsEvent(zombie, zombie.damageSources().generic(), new ArrayList<>(), 0, true);
            ConfiguredAcquisition.drops(disabledEvent);
            helper.assertTrue(disabledEvent.getDrops().stream().noneMatch(e -> ItemCsgoBox.boxId(e.getItem()).equals(box.id())), "Disabled box still dropped");
        });
        helper.succeed();
    }
}
