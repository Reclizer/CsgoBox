package com.reclizer.csgobox.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomItem {
    public static int randonmItemsGrade(Random random1 ,int[] array, Player player){

        int luck=(int)player.getLuck()*2;

        //概率
        int blue=array[4];//蓝
        int purple=array[3];//紫
        int pink=array[2];//粉
        int red=array[1];//红
        int gold=array[0];//金


        int randomAdd=0;

        int sumGrade=gold+red+pink+purple+blue;


        int r=random1.nextInt(sumGrade);


        randomAdd+=gold+luck;
        if(r<=randomAdd){
            return 5;
        }
        randomAdd+=red+luck;
        if(r<=randomAdd){
            return 4;
        }
        randomAdd+=pink+luck;
        if(r<=randomAdd){
            return 3;
        }
        randomAdd+=purple+luck;
        if(r<=randomAdd){
            return 2;
        }
        return 1;
    }


    public static ItemStack randomItems(Random random,int grade, Map<ItemStack , Integer> map){

        Map<ItemStack , Integer> ItemsMap=map;
        List<ItemStack> items = new ArrayList<>();

        for(Map.Entry<ItemStack, Integer> entry : ItemsMap.entrySet()){
            if(entry.getValue()==grade){
                items.add(entry.getKey());
            }
        }

        int i=0;
        if(items.size()>1){
            i=random.nextInt(0,items.size()-1);
        }

        ItemStack newItem=items.get(i);

        return newItem;
    }
}
