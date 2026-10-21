package com.reclizer.csgobox.utils;

import net.minecraft.world.item.ItemStack;

public final class BoxKeys {
    private BoxKeys() {}
    public static ItemStack parse(String value) { return value.isBlank() ? ItemStack.EMPTY : ItemNBT.parseStrict(value); }
    /** An ID-only key accepts that item; a sampled key with NBT also requires its configured tags. */
    public static boolean matches(ItemStack required, ItemStack candidate) {
        return !required.isEmpty() && !candidate.isEmpty() && candidate.is(required.getItem())
                && (!required.hasTag() || ItemStack.isSameItemSameTags(required, candidate));
    }
}
