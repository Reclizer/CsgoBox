package com.reclizer.csgobox.compat.jei;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Stable content keys let unchanged recipes be reused when JEI updates at runtime. */
public record AcquisitionRecipe(String fingerprint, Kind kind, ItemStack output, List<ItemStack> inputs,
                                String source, float chance, int level, int uses, int xp) {
    public enum Kind { CRAFTING, DROP, TRADE }
    public static ItemStack box(String id) {
        return ItemCsgoBox.setBoxId(id, new ItemStack(ModItems.ITEM_CSGOBOX.get()));
    }
    public static List<AcquisitionRecipe> build(Collection<AcquisitionSummary> pools) {
        var result = new ArrayList<AcquisitionRecipe>();
        for (var pool : pools) {
            add(result, pool.id()+"/box", box(pool.id()), pool.boxRules());
            if (!pool.key().isEmpty()) add(result, pool.id()+"/key", ItemNBT.parseStrict(pool.key()), pool.keyRules());
        }
        return List.copyOf(result);
    }
    private static ItemStack count(ItemStack base, int count) { var copy = base.copy(); copy.setCount(count); return copy; }
    private static void add(List<AcquisitionRecipe> result, String id, ItemStack output, AcquisitionDefinition rules) {
        id += "/"+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem());
        if (rules.crafting().enabled()) {
            var c = rules.crafting();
            result.add(new AcquisitionRecipe(id+"/craft/"+output.getTag()+c, Kind.CRAFTING, count(output,c.count()),
                    c.ingredients().stream().map(v -> v.isEmpty() ? ItemStack.EMPTY : ItemNBT.parseStrict(v)).toList(), "",0,0,0,0));
        }
        if (rules.drop().enabled()) {
            var d = rules.drop();
            for (String entity : d.entities().stream().distinct().toList())
                result.add(new AcquisitionRecipe(id+"/drop/"+output.getTag()+d+entity, Kind.DROP, count(output,d.count()),
                        List.of(),entity,d.chance(),0,0,0));
        }
        if (rules.trade().enabled()) {
            var t = rules.trade();
            result.add(new AcquisitionRecipe(id+"/trade/"+output.getTag()+t, Kind.TRADE, count(output,t.count()),
                    List.of(ItemNBT.parseStrict(t.cost()), t.secondCost().isEmpty() ? ItemStack.EMPTY : ItemNBT.parseStrict(t.secondCost())),
                    t.profession(),0,t.level(),t.maxUses(),t.xp()));
        }
    }
}
