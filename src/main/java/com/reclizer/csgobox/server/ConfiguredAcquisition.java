package com.reclizer.csgobox.server;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.utils.*;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Live server recipes/drop rules and factories for future villager offers. Existing offers remain vanilla data. */
public final class ConfiguredAcquisition {
    private static List<Recipe<?>> managedRecipes = List.of();
    private ConfiguredAcquisition() {}

    public static ItemStack output(BoxDefinition box, boolean key, int count) {
        ItemStack stack = key ? BoxKeys.parse(box.key())
                : ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get()));
        if (!stack.isEmpty()) stack.setCount(count);
        return stack;
    }

    public static void validate(BoxDefinition box) {
        ItemStack key = BoxKeys.parse(box.key());
        if (!key.isEmpty() && key.getCount() != 1) throw new IllegalArgumentException("editor.csgobox.key_count_one");
        for (boolean isKey : List.of(false, true)) {
            if (isKey && key.isEmpty()) continue;
            var rules = isKey ? box.keyAcquisition() : box.acquisition();
            int max = output(box, isKey, 1).getMaxStackSize();
            if (rules.crafting().count() > max || rules.drop().count() > max || rules.trade().count() > max)
                throw new IllegalArgumentException("editor.csgobox.bad_acquisition_count");
            for (String ingredient : rules.crafting().ingredients())
                if (!ingredient.isEmpty() && ItemNBT.parseStrict(ingredient).getCount() != 1)
                    throw new IllegalArgumentException("editor.csgobox.recipe_count_one");
            if (rules.drop().enabled()) {
                for (String entity : rules.drop().entities())
                    if (!ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(entity)))
                        throw new IllegalArgumentException("Unknown drop entity: " + entity);
            }
            if (rules.trade().enabled()) {
                var id = new ResourceLocation(rules.trade().profession());
                var profession = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(id);
                if (!ForgeRegistries.VILLAGER_PROFESSIONS.containsKey(id)
                        || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT)
                    throw new IllegalArgumentException("editor.csgobox.bad_profession");
            }
        }
    }

    public static ResourceLocation recipeId(BoxDefinition box, boolean key) {
        return new ResourceLocation("csgobox", "configured/" + (key ? "key/" : "box/") + box.id().replace(':', '/'));
    }

    private static final class ConfiguredRecipe extends ShapedRecipe {
        ConfiguredRecipe(ResourceLocation id, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
            super(id, "csgobox", CraftingBookCategory.MISC, width, height, ingredients, result);
        }
    }

    public static ShapedRecipe recipe(BoxDefinition box, boolean key) {
        var rule = (key ? box.keyAcquisition() : box.acquisition()).crafting();
        if (!rule.enabled() || key && box.key().isEmpty()) return null;
        int minX = 3, maxX = -1, minY = 3, maxY = -1;
        for (int i = 0; i < 9; i++) if (!rule.ingredients().get(i).isEmpty()) {
            minX = Math.min(minX, i % 3); maxX = Math.max(maxX, i % 3);
            minY = Math.min(minY, i / 3); maxY = Math.max(maxY, i / 3);
        }
        if (maxX < 0) throw new IllegalArgumentException("editor.csgobox.bad_recipe");
        var ingredients = NonNullList.<Ingredient>create();
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            String value = rule.ingredients().get(y * 3 + x);
            if (value.isEmpty()) ingredients.add(Ingredient.EMPTY);
            else {
                ItemStack sample = ItemNBT.parseStrict(value);
                ingredients.add(sample.hasTag() ? StrictNBTIngredient.of(sample) : Ingredient.of(sample.getItem()));
            }
        }
        return new ConfiguredRecipe(recipeId(box, key), maxX - minX + 1, maxY - minY + 1,
                ingredients, output(box, key, rule.count()));
    }

    public static MerchantOffer offer(BoxDefinition box, boolean key) {
        var trade = (key ? box.keyAcquisition() : box.acquisition()).trade();
        if (!trade.enabled() || key && box.key().isEmpty()) return null;
        return new MerchantOffer(ItemNBT.parseStrict(trade.cost()),
                trade.secondCost().isEmpty() ? ItemStack.EMPTY : ItemNBT.parseStrict(trade.secondCost()),
                output(box, key, trade.count()), trade.maxUses(), trade.xp(), 0.05F);
    }

    private record ConfiguredTrade(String poolId, boolean key) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(Entity trader, RandomSource random) {
            var box = CsgoBoxManage.find(poolId);
            return box == null ? null : offer(box, key);
        }
    }

    public static void clearTrades() {
        VillagerTrades.TRADES.values().forEach(levels -> levels.replaceAll((level, entries) ->
                Arrays.stream(entries).filter(entry -> !(entry instanceof ConfiguredTrade)).toArray(VillagerTrades.ItemListing[]::new)));
    }

    public static void clear() { clearTrades(); managedRecipes = List.of(); }

    public static void refreshCrafting(ServerPlayer player) {
        player.inventoryMenu.slotsChanged(player.inventoryMenu.getCraftSlots());
        if (player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu)
            menu.slotsChanged(menu.getSlot(1).container);
    }

    /** Build first, then replace only our contributions; repeated saves/reloads never accumulate entries. */
    public static void refresh(MinecraftServer server) {
        var recipes = new ArrayList<Recipe<?>>();
        for (var box : CsgoBoxManage.boxes()) for (boolean key : List.of(false, true)) {
            var recipe = recipe(box, key);
            if (recipe != null) recipes.add(recipe);
        }
        // Keep the previous list across vanilla /reload, which replaces RecipeManager before this hook.
        var old = managedRecipes;
        var ids = recipes.stream().map(Recipe::getId).collect(java.util.stream.Collectors.toSet());
        var all = new ArrayList<Recipe<?>>();
        server.getRecipeManager().getRecipes().stream()
                .filter(r -> !(r instanceof ConfiguredRecipe) && !ids.contains(r.getId())).forEach(all::add);
        all.addAll(recipes);
        server.getPlayerList().getPlayers().forEach(player -> player.resetRecipes(old));
        server.getRecipeManager().replaceRecipes(all);
        managedRecipes = List.copyOf(recipes);
        server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(all));
        server.getPlayerList().getPlayers().forEach(player -> {
            player.awardRecipes(recipes);
            refreshCrafting(player);
        });
        clearTrades();
        for (var box : CsgoBoxManage.boxes()) for (boolean key : List.of(false, true)) {
            if (key && box.key().isEmpty()) continue;
            var trade = (key ? box.keyAcquisition() : box.acquisition()).trade();
            if (!trade.enabled()) continue;
            var profession = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(new ResourceLocation(trade.profession()));
            var levels = VillagerTrades.TRADES.computeIfAbsent(profession, ignored -> new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>());
            var entries = new ArrayList<>(Arrays.asList(levels.getOrDefault(trade.level(), new VillagerTrades.ItemListing[0])));
            entries.add(new ConfiguredTrade(box.id(), key));
            levels.put(trade.level(), entries.toArray(VillagerTrades.ItemListing[]::new));
        }
    }

    public static void unlockRecipes(ServerPlayer player) {
        player.awardRecipes(player.server.getRecipeManager().getRecipes().stream().filter(r -> r instanceof ConfiguredRecipe).toList());
    }

    public static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        for (var box : CsgoBoxManage.boxes()) for (boolean key : List.of(false, true)) {
            var drop = (key ? box.keyAcquisition() : box.acquisition()).drop();
            if (drop.enabled() && drop.entities().contains(entityId) && entity.getRandom().nextFloat() < drop.chance()) {
                var stack = output(box, key, drop.count());
                if (!stack.isEmpty()) event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
            }
        }
    }
}
