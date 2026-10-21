package com.reclizer.csgobox.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemNBT {
    private ItemNBT() {}

    public static String getStacksData(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Cannot serialize an empty reward");
        String data = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if (stack.hasTag()) data += ".withTags" + stack.getTag();
        if (stack.getCount() != 1) data += ".withCount(" + stack.getCount() + ")";
        return data;
    }

    /** The vanilla SNBT parser preserves quoted modifier names inside tags. */
    public static ItemStack parseStrict(String data) {
        if (data == null || data.isBlank() || data.length() > 8192)
            throw new IllegalArgumentException("Missing or oversized item definition");
        int tags = data.indexOf(".withTags");
        int count = data.indexOf(".withCount(");
        int end = tags < 0 ? data.length() : tags;
        if (count >= 0) end = Math.min(end, count);
        ResourceLocation id = ResourceLocation.tryParse(data.substring(0, end).trim());
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id))
            throw new IllegalArgumentException("Unknown item: " + data.substring(0, end));
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        if (stack.isEmpty()) throw new IllegalArgumentException("Air is not a reward");
        StringReader reader = new StringReader(data.substring(end));
        boolean hasTags = false;
        boolean hasCount = false;
        while (reader.canRead()) {
            reader.skipWhitespace();
            if (!reader.canRead()) break;
            if (!hasTags && reader.getRemaining().startsWith(".withTags")) {
                reader.setCursor(reader.getCursor() + 9);
                reader.skipWhitespace();
                try {
                    stack.setTag(new TagParser(reader).readStruct());
                } catch (CommandSyntaxException ex) {
                    throw new IllegalArgumentException("Invalid item NBT", ex);
                }
                hasTags = true;
            } else if (!hasCount && reader.getRemaining().startsWith(".withCount(")) {
                reader.setCursor(reader.getCursor() + 11);
                int start = reader.getCursor();
                while (reader.canRead() && Character.isDigit(reader.peek())) reader.skip();
                if (start == reader.getCursor() || !reader.canRead() || reader.read() != ')')
                    throw new IllegalArgumentException("Invalid item count");
                int amount = Integer.parseInt(reader.getString().substring(start, reader.getCursor() - 1));
                if (amount < 1 || amount > stack.getMaxStackSize())
                    throw new IllegalArgumentException("Item count must be within its stack limit");
                stack.setCount(amount);
                hasCount = true;
            } else {
                throw new IllegalArgumentException("Unexpected item suffix: " + reader.getRemaining());
            }
        }
        com.reclizer.csgobox.compat.TaczCompat.validate(stack);
        return stack;
    }

    public static ItemStack getStacks(String data) {
        try { return parseStrict(data); }
        catch (IllegalArgumentException ex) { return ItemStack.EMPTY; }
    }
}
