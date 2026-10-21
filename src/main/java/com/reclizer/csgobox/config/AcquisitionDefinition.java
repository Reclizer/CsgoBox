package com.reclizer.csgobox.config;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Independent acquisition methods for a pool's box or key. Empty crafting cells use an empty string. */
public record AcquisitionDefinition(Crafting crafting, Drop drop, Trade trade) {
    public AcquisitionDefinition {
        crafting = crafting == null ? new Crafting(false, Collections.nCopies(9, ""), 1) : crafting;
        drop = drop == null ? new Drop(false, 0, List.of(), 1) : drop;
        trade = trade == null ? new Trade(false, "minecraft:weaponsmith", 1, "minecraft:emerald", "", 1, 12, 1) : trade;
    }
    public record Crafting(boolean enabled, List<String> ingredients, int count) {
        public Crafting { ingredients = ingredients == null ? Collections.nCopies(9, "") : List.copyOf(ingredients); }
    }
    public record Drop(boolean enabled, float chance, List<String> entities, int count) {
        public Drop { entities = entities == null ? List.of() : List.copyOf(entities); }
    }
    public record Trade(boolean enabled, String profession, int level, String cost, String secondCost,
                        int count, int maxUses, int xp) {}

    public static AcquisitionDefinition disabled() { return new AcquisitionDefinition(null, null, null); }
    public static AcquisitionDefinition legacy(float chance, List<String> entities) {
        return new AcquisitionDefinition(null, new Drop(chance > 0, chance, entities, 1), null);
    }
    public void validate(Predicate<String> validItem) {
        if (crafting.ingredients().size() != 9 || crafting.count() < 1 || crafting.count() > 64)
            throw new IllegalArgumentException("editor.csgobox.bad_recipe");
        int ingredients = 0;
        for (String item : crafting.ingredients()) {
            if (!item.isEmpty()) { validateItem(item, validItem); ingredients++; }
        }
        if (crafting.enabled() && ingredients == 0) throw new IllegalArgumentException("editor.csgobox.bad_recipe");
        if (!Float.isFinite(drop.chance()) || drop.chance() < 0 || drop.chance() > 1
                || drop.count() < 1 || drop.count() > 64 || drop.entities().size() > 256
                || drop.entities().stream().anyMatch(id -> !BoxDefinition.validId(id))
                || drop.enabled() && drop.entities().isEmpty())
            throw new IllegalArgumentException("editor.csgobox.bad_acquisition_drop");
        if (!BoxDefinition.validId(trade.profession()) || trade.level() < 1 || trade.level() > 5
                || trade.count() < 1 || trade.count() > 64 || trade.maxUses() < 1 || trade.maxUses() > 9999
                || trade.xp() < 0 || trade.xp() > 100 || trade.cost() == null || trade.secondCost() == null)
            throw new IllegalArgumentException("editor.csgobox.bad_trade");
        if (!trade.cost().isEmpty()) validateItem(trade.cost(), validItem);
        if (!trade.secondCost().isEmpty()) validateItem(trade.secondCost(), validItem);
        if (trade.enabled() && trade.cost().isEmpty()) throw new IllegalArgumentException("editor.csgobox.bad_trade");
    }
    private static void validateItem(String item, Predicate<String> validItem) {
        if (item.isBlank() || item.length() > 8192 || !validItem.test(item))
            throw new IllegalArgumentException("editor.csgobox.bad_acquisition_item");
    }
}
