package com.reclizer.csgobox.utils;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class ItemNBT {
    public static String readTags(ItemStack stack){

        if(stack.getTag()==null){
            return null;
        }
        return stack.getTag().toString();
    }

    public static String tagsItemName(String itemDate){
        if(!itemDate.contains(".withTags")){
            return itemDate;
        }
        String itemName;
        int withTagsIndex = itemDate.indexOf(".withTags");
        if (withTagsIndex != -1) {
            itemName = itemDate.substring(0, withTagsIndex).trim();
            return itemName;
        }
        return itemDate;
    }

    public static String tagsItemDate(String itemDate){
        if(!itemDate.contains(".withTags")){
            return null;
        }
        String itemName;
        int withTagsIndex = itemDate.indexOf(".withTags");
        if (withTagsIndex != -1) {
            itemName = itemDate.substring(withTagsIndex + ".withTags".length()).trim();
            return itemName;
        }
        return null;
    }



    public static  String getStacksData(ItemStack stack){

        if(stack.isEmpty()){
            return null;
        }
        String data=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if(stack.getTag()!=null){
            String tag=ItemNBT.readTags(stack);
            data=data+".withTags"+tag;

        }
        return data;
    }
    public static ItemStack getStacks(String itemDate){

        String itemName=itemDate;



        String itemTags=null;




        if(itemDate.contains(".withTags")){
            itemName=ItemNBT.tagsItemName(itemDate);
            itemTags=ItemNBT.tagsItemDate(itemDate);

        }

        ResourceLocation res=new ResourceLocation(itemName);
        if(ForgeRegistries.ITEMS.getValue(res)==null){
            return null;
        }
        ItemStack stack1=new ItemStack(ForgeRegistries.ITEMS.getValue(res));
        if(!isModLoaded("crafttweaker")){
            return stack1;
        }

        if(itemTags!=null&&itemTags.contains("{")){


            CraftWeakerNBT.setTags(stack1, itemTags);


        }


        return stack1;

    }


    public static boolean isModLoaded(String modid) {
        try {
            return ModList.get().isLoaded(modid);
        } catch (Throwable e) {
            return false;
        }
    }



}
