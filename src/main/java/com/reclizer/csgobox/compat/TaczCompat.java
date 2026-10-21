package com.reclizer.csgobox.compat;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Data-only optional integration: no TaCZ classes are loaded when the mod is absent. */
public final class TaczCompat {
    private static final ResourceLocation GUN = new ResourceLocation("tacz", "modern_kinetic_gun");
    private TaczCompat() {}

    public static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && GUN.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static String gunId(ItemStack stack) {
        return isGun(stack) && stack.hasTag() ? stack.getTag().getString("GunId") : "";
    }

    public static void validate(ItemStack stack) {
        if (isGun(stack) && (!stack.hasTag() || !stack.getTag().contains("GunId", Tag.TAG_STRING)
                || ResourceLocation.tryParse(gunId(stack)) == null || gunId(stack).isBlank()))
            throw new IllegalArgumentException("editor.csgobox.tacz_sample_required");
    }
}
