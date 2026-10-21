package com.reclizer.csgobox.compat;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

    /** TaCZ's Item.getName uses a client-only index; read its common pack metadata on the server. */
    public static Component rewardName(ItemStack stack) {
        if (stack.hasCustomHoverName() || !isGun(stack)) return stack.getHoverName();
        try {
            var api = Class.forName("com.tacz.guns.api.TimelessAPI");
            var index = (java.util.Optional<?>) api.getMethod("getCommonGunIndex", ResourceLocation.class)
                    .invoke(null, ResourceLocation.tryParse(gunId(stack)));
            if (index.isPresent()) {
                Object pojo = index.get().getClass().getMethod("getPojo").invoke(index.get());
                String key = (String) pojo.getClass().getMethod("getName").invoke(pojo);
                if (key != null && !key.isBlank()) return Component.translatable(key);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Unknown API versions can still show a useful model identifier without client class loading.
        }
        return Component.literal(gunId(stack));
    }

    public static void validate(ItemStack stack) {
        if (isGun(stack) && (!stack.hasTag() || !stack.getTag().contains("GunId", Tag.TAG_STRING)
                || ResourceLocation.tryParse(gunId(stack)) == null || gunId(stack).isBlank()))
            throw new IllegalArgumentException("editor.csgobox.tacz_sample_required");
    }
}
