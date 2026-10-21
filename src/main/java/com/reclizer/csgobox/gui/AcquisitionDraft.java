package com.reclizer.csgobox.gui;

import com.reclizer.csgobox.config.AcquisitionDefinition;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Unsaved editor text remains text until validation, so partial numeric input is harmless. */
final class AcquisitionDraft {
    boolean crafting, dropping, trading;
    final List<String> ingredients = new ArrayList<>();
    String craftCount, dropChance, entities, dropCount, profession, level, cost, secondCost;
    String costCount, secondCount, tradeCount, uses, xp;

    AcquisitionDraft(AcquisitionDefinition value) {
        crafting = value.crafting().enabled(); ingredients.addAll(value.crafting().ingredients());
        craftCount = Integer.toString(value.crafting().count());
        dropping = value.drop().enabled();
        dropChance = new java.math.BigDecimal(Float.toString(value.drop().chance())).movePointRight(2).stripTrailingZeros().toPlainString();
        entities = String.join(", ", value.drop().entities()); dropCount = Integer.toString(value.drop().count());
        var trade = value.trade();
        trading = trade.enabled(); profession = trade.profession(); level = Integer.toString(trade.level());
        cost = trade.cost(); secondCost = trade.secondCost();
        costCount = Integer.toString(ItemNBT.getStacks(cost).getCount());
        secondCount = secondCost.isEmpty() ? "1" : Integer.toString(ItemNBT.getStacks(secondCost).getCount());
        tradeCount = Integer.toString(trade.count()); uses = Integer.toString(trade.maxUses()); xp = Integer.toString(trade.xp());
    }
    AcquisitionDefinition definition() {
        try {
            return new AcquisitionDefinition(new AcquisitionDefinition.Crafting(crafting, ingredients, Integer.parseInt(craftCount)),
                    new AcquisitionDefinition.Drop(dropping, Float.parseFloat(dropChance) / 100F,
                            Arrays.stream(entities.split("[,;\\s]+")).filter(s -> !s.isBlank()).distinct().toList(), Integer.parseInt(dropCount)),
                    new AcquisitionDefinition.Trade(trading, profession.trim(), Integer.parseInt(level),
                            counted(cost, costCount), counted(secondCost, secondCount), Integer.parseInt(tradeCount), Integer.parseInt(uses), Integer.parseInt(xp)));
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("editor.csgobox.bad_acquisition_numbers"); }
    }
    private static String counted(String value, String count) {
        if (value.isEmpty()) return "";
        ItemStack sample = ItemNBT.parseStrict(value);
        int amount = Integer.parseInt(count);
        if (amount < 1 || amount > sample.getMaxStackSize()) throw new IllegalArgumentException("editor.csgobox.bad_count");
        sample.setCount(amount);
        return ItemNBT.getStacksData(sample);
    }
}
