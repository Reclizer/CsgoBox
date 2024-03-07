package com.reclizer.csgobox.gui;


import com.reclizer.csgobox.CsgoBox;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;



public class RecModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, CsgoBox.MODID);




    public static final RegistryObject<MenuType<CsgoBoxCraftMenu>> CSGO_BOX_CRAFT = MENUS.register("csgo_box_craft", () -> IForgeMenuType.create(CsgoBoxCraftMenu::new));


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }



}

