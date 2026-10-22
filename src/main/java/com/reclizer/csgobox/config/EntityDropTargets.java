package com.reclizer.csgobox.config;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Comparator;
import java.util.List;

/** Registered living drop targets, including mod attributes and creatures in MISC. No entity construction. */
public final class EntityDropTargets {
    private EntityDropTargets() {}
    public static boolean eligible(EntityType<?> type) {
        return type!=EntityType.PLAYER && type!=EntityType.ARMOR_STAND && DefaultAttributes.hasSupplier(type);
    }
    public static List<EntityType<?>> types() {
        return ForgeRegistries.ENTITY_TYPES.getValues().stream().filter(EntityDropTargets::eligible)
                .sorted(Comparator.comparing(type->ForgeRegistries.ENTITY_TYPES.getKey(type).toString())).toList();
    }
}
