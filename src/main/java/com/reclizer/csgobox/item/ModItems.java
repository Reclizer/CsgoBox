package com.reclizer.csgobox.item;

import com.reclizer.csgobox.CsgoBox;


import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CsgoBox.MODID);

    public static void registerTab(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    public static final RegistryObject<CreativeModeTab> EQUIPMENT_TAB = TABS.register(CsgoBox.MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + CsgoBox.MODID + ".cs_tab"))
            .icon(() -> new ItemStack(ModItems.ITEM_CSGO_KEY0.get()))
            .displayItems((enabledFeatures, entries) -> {

                entries.accept(ModItems.ITEM_CSGO_CRAFT.get());

                entries.accept(ModItems.ITEM_CSGOBOX.get());

                entries.accept(ModItems.ITEM_CSGO_KEY0.get());
                entries.accept(ModItems.ITEM_CSGO_KEY1.get());
                entries.accept(ModItems.ITEM_CSGO_KEY2.get());
                entries.accept(ModItems.ITEM_CSGO_KEY3.get());
                for (var appearance : com.reclizer.csgobox.config.BoxAppearance.values())
                    entries.accept(EmptyBoxItem.create(appearance));

                //entries.accept(ModItems.ITEM_CSGO_TEST.get());

                for (var pool : BoxCatalog.entries()) {
                    entries.accept(ItemCsgoBox.setBoxId(pool.id(), new ItemStack(ModItems.ITEM_CSGOBOX.get())));
                }
            })
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .build());


    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CsgoBox.MODID);
    public static final RegistryObject<Item> ITEM_CSGOBOX=ITEMS.register("csgo_box",  ItemCsgoBox::new);
    public static final RegistryObject<Item> ITEM_EMPTY_BOX=ITEMS.register("empty_box", EmptyBoxItem::new);

    public static final RegistryObject<Item> ITEM_CSGO_KEY0=ITEMS.register("csgo_key0",  ItemCsgoKey::new);
    public static final RegistryObject<Item> ITEM_CSGO_KEY1=ITEMS.register("csgo_key1",  ItemCsgoKey::new);
    public static final RegistryObject<Item> ITEM_CSGO_KEY2=ITEMS.register("csgo_key2",  ItemCsgoKey::new);
    public static final RegistryObject<Item> ITEM_CSGO_KEY3=ITEMS.register("csgo_key3",  ItemCsgoKey::new);
    public static final RegistryObject<Item> ITEM_CSGO_CRAFT=ITEMS.register("csgo_box_craft",  ItemOpenBox::new);



    public static  void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
