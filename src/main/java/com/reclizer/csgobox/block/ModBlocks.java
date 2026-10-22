package com.reclizer.csgobox.block;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CsgoBox.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CsgoBox.MODID);
    public static final RegistryObject<PlacedBoxBlock> BOX = BLOCKS.register("csgo_box", PlacedBoxBlock::new);
    public static final RegistryObject<BlockEntityType<PlacedBoxEntity>> BOX_ENTITY = ENTITIES.register("csgo_box",
            () -> BlockEntityType.Builder.of(PlacedBoxEntity::new, BOX.get()).build(null));

    private ModBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
}
