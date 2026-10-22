package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Opt-in registry fixture, never included in the release JAR or spawned into a world. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class EntityPickerFixture {
    static EntityType<Zombie> creature;
    @SubscribeEvent public static void register(RegisterEvent event) {
        if (!Boolean.getBoolean("csgobox.entityPickerChecks") || !event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_TYPES)) return;
        creature=EntityType.Builder.<Zombie>of(Zombie::new,MobCategory.MONSTER).sized(.6F,1.95F).build("selector_fixture:creature");
        event.register(ForgeRegistries.Keys.ENTITY_TYPES,helper -> helper.register(new ResourceLocation("selector_fixture","creature"),creature));
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        if (creature!=null) event.put(creature,Zombie.createAttributes().build());
    }
}
