package com.reclizer.csgobox.utils;

import com.blamejared.crafttweaker.api.data.IData;
import com.blamejared.crafttweaker.api.data.MapData;
import com.blamejared.crafttweaker.api.data.converter.JSONConverter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class CraftWeakerNBT {

    public static ItemStack setTags(ItemStack stack, String tagStr){


        JsonObject jsonObject = JsonParser.parseString(tagStr).getAsJsonObject();
        MapData mapData= JSONConverter.convert(jsonObject);
        Map<String, IData> map=mapData.asMap();
        CompoundTag tag=stack.getOrCreateTagElement(ItemStack.TAG_DISPLAY);

        for(String key:map.keySet()){
            tag.put(key,map.get(key).getInternal());
        }

        stack.setTag(tag);

        return stack;
    }
}
