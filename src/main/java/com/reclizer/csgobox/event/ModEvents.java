package com.reclizer.csgobox.event;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.CsgoBoxManage;


import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.utils.BlurHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

import static net.minecraft.world.entity.MobType.UNDEAD;

@Mod.EventBusSubscriber(modid = CsgoBox.MODID)
public class ModEvents {


    @SubscribeEvent
    public static void LivingDeadEvents(LivingDeathEvent event) {
        LivingEntity mob=event.getEntity();
        String entityType=ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();

        if(CsgoBoxManage.BOX==null){
            return;
        }

            for (ItemCsgoBox.BoxInfo info : CsgoBoxManage.BOX) {
                if(info.dropEntity==null){
                    return;
                }
                if(info.dropRandom>0){
                    Random random = new Random();

                    if(info.dropRandom>(1.00F-random.nextFloat(1))&&info.dropEntity!=null&&info.dropEntity.contains(entityType)){
                        ItemStack stack = new ItemStack(ModItems.ITEM_CSGOBOX.get());
                        ItemCsgoBox.setBoxInfo(info, stack);
                        mob.spawnAtLocation(stack);
                    }
                }
            }
    }






}
