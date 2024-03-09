package com.reclizer.csgobox;

import com.mojang.logging.LogUtils;
import com.reclizer.csgobox.gui.CsgoBoxCraftScreen;
import com.reclizer.csgobox.gui.RecModScreens;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.gui.RecModMenus;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.packet.Networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CsgoBox.MODID)
public class CsgoBox {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "csgobox";
    // Directly reference a slf4j logger
    //public static IProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "csgobox" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "csgobox" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "csgobox:example_block", combining the namespace and path

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab


    public CsgoBox() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading

        modEventBus.addListener(this::commonSetup);
        ModSounds.SOUNDS.register(modEventBus);
        RecModMenus.register(modEventBus);
        ModItems.register(modEventBus);
        ModItems.registerTab(modEventBus);



        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        Path configPath = FMLPaths.CONFIGDIR.get(); // 获取Minecraft配置目录
        Path folderPath = configPath.resolve("csbox");
        try {
            // 创建文件夹（如果不存在）
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            String content ="[\n" +
                    "  {\n" +
                    "    \"name\": \"Weapons Supply Box\",\n" +
                    "    \"key\": \"csgobox:csgo_key0\",\n" +
                    "    \"drop\": 0.12,\n" +
                    "    \"random\": [\n" +
                    "      2,\n" +
                    "      5,\n" +
                    "      6,\n" +
                    "      20,\n" +
                    "      625\n" +
                    "    ],\n" +
                    "    \"entity\": [\n" +
                    "      \"minecraft:zombie\",\n" +
                    "      \"minecraft:skeleton\"\n" +
                    "    ],\n" +
                    "    \"grade1\": [\n" +
                    "      \"minecraft:stone_sword.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_shovel.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_pickaxe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_hoe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:iron_sword.withTags{Damage:0}\"\n" +
                    "    ],\n" +
                    "    \"grade2\": [\n" +
                    "      \"minecraft:golden_sword.withTags{Damage:0}\",\n" +
                    "      \"minecraft:golden_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:golden_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:golden_pickaxe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:golden_shovel.withTags{Damage:0}\"\n" +
                    "    ],\n" +
                    "    \"grade3\": [\n" +
                    "      \"minecraft:diamond_shovel.withTags{Damage:0}\",\n" +
                    "      \"minecraft:diamond_pickaxe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:diamond_hoe.withTags{Damage:0}\"\n" +
                    "    ],\n" +
                    "    \"grade4\": [\n" +
                    "      \"minecraft:diamond_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:diamond_sword.withTags{Damage:0}\"\n" +
                    "    ],\n" +
                    "    \"grade5\": [\n" +
                    "      \"minecraft:netherite_sword.withTags{Damage:0,Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:4s},{id:\\\"minecraft:fire_aspect\\\",lvl:1s},{id:\\\"minecraft:sweeping\\\",lvl:2s}],RepairCost:7}\",\n" +
                    "      \"minecraft:netherite_axe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:netherite_pickaxe.withTags{Damage:0}\",\n" +
                    "      \"minecraft:netherite_shovel.withTags{Damage:0}\",\n" +
                    "      \"minecraft:netherite_hoe.withTags{Damage:0}\"\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]";

            if (!Files.exists(folderPath.resolve("box.json"))) {
                Path filePath = folderPath.resolve("box.json");
                // 创建文件并写入内容new FileWriter(filePath.toFile())
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath.toFile()), "UTF-8"))) {

                    writer.write(content);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        event.enqueueWork(() -> {
            try {
                CsgoBoxManage.loadConfigBox();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // Some common setup code
        Networking.registerMessages();
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

//        if (Config.logDirtBlock)
//            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
//
//        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
//
//        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {


            event.enqueueWork(RecModScreens::clientLoad);

            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
