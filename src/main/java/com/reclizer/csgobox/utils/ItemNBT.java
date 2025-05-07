package com.reclizer.csgobox.utils;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemNBT {
    public static String readTags(ItemStack stack){
        return stack.getTag().toString();
    }

    public static String tagsItemName(String itemDate){
        String itemName=itemDate;

        if (itemDate.contains(".withTags")){
            itemName=itemDate.substring(0,itemDate.indexOf(".withTags"));
        }
        
        if (itemDate.contains(".withCount")) {
            itemName = itemName.substring(0, itemName.indexOf(".withCount"));
        }
        
        return itemName;
    }

    public static String tagsItemDate(String itemDate){
        String nbt="";
        if (itemDate.contains(".withTags")){
            nbt=itemDate.substring(itemDate.indexOf(".withTags")+9);
            
            // If there's a withCount after the tags, remove it
            if (nbt.contains(".withCount")) {
                nbt = nbt.substring(0, nbt.indexOf(".withCount"));
            }
        }
        return nbt;
    }
    
    public static int getItemCount(String itemDate) {
        // Default count is 1
        int count = 1;
        
        // Use regex to extract count from pattern like .withCount(X)
        if (itemDate.contains(".withCount")) {
            Pattern pattern = Pattern.compile("\\.withCount\\((\\d+)\\)");
            Matcher matcher = pattern.matcher(itemDate);
            if (matcher.find()) {
                try {
                    count = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    // If parsing fails, use default value of 1
                }
            }
        }
        
        return count;
    }

    public static String getStacksData(ItemStack stack){
        if(stack.isEmpty()){
            return null;
        }
        String data = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if(stack.getTag() != null){
            String tag = ItemNBT.readTags(stack);
            data = data + ".withTags" + tag;
        }
        
        // Add count information if stack size is more than 1
        if(stack.getCount() > 1) {
            data = data + ".withCount(" + stack.getCount() + ")";
        }
        
        return data;
    }
    
    public static ItemStack getStacks(String itemDate){
        String itemName = tagsItemName(itemDate);
        String nbt = tagsItemDate(itemDate);
        int count = getItemCount(itemDate);
        
        String modId = null;
        String items = null;
        if(itemName.contains(":")){
            String[] parts = itemName.split(":");
            modId = parts[0];
            items = parts[1];
        }
        
        if (modId != null && items != null && isModLoaded(modId)) {
            ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(modId, items)));
            
            // Apply the count
            itemStack.setCount(count);
            
            if (!nbt.isEmpty()) {
                try {
                    CraftWeakerNBT.setTags(itemStack, nbt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            return itemStack;
        }
        
        return ItemStack.EMPTY;
    }

    public static boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}
