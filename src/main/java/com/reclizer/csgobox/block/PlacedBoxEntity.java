package com.reclizer.csgobox.block;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.config.BoxAppearance;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

/** Placed boxes keep the same stable identity as inventory boxes, never a reward snapshot. */
public final class PlacedBoxEntity extends BlockEntity {
    private String poolId = "";
    private boolean claimed;

    public PlacedBoxEntity(BlockPos pos, BlockState state) { super(ModBlocks.BOX_ENTITY.get(), pos, state); }
    public String poolId() { return poolId; }

    public void setPoolId(String id) {
        poolId = BoxDefinition.validId(id) ? id : "";
        setChanged();
        refreshAppearance();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Refresh loaded boxes after edits/reloads without storing a second configuration snapshot. */
    public void refreshAppearance() {
        if (level == null || level.isClientSide || isRemoved()) return;
        BoxDefinition definition = CsgoBoxManage.find(poolId);
        BlockState state = getBlockState();
        if (definition == null || !state.is(ModBlocks.BOX.get())) return;
        var appearance = BoxAppearance.byId(definition.appearance());
        if (state.getValue(PlacedBoxBlock.APPEARANCE) != appearance)
            level.setBlock(worldPosition, state.setValue(PlacedBoxBlock.APPEARANCE, appearance), 3);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacedBoxEntity box) {
        // Spread checks across ticks; chunk meshes only change when the chosen appearance changes.
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) box.refreshAppearance();
    }

    /** Also rejects reentrant opens during neighbor updates while the block is being removed. */
    public boolean claim() {
        if (claimed || isRemoved()) return false;
        claimed = true;
        return true;
    }
    public void releaseClaim() { claimed = false; }

    public ItemStack asItem() {
        if (claimed || !BoxDefinition.validId(poolId)) return ItemStack.EMPTY;
        return ItemCsgoBox.setBoxId(poolId, new ItemStack(ModItems.ITEM_CSGOBOX.get()));
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(ItemCsgoBox.POOL_ID_TAG, poolId);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        String id = tag.getString(ItemCsgoBox.POOL_ID_TAG);
        poolId = BoxDefinition.validId(id) ? id : "";
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ItemCsgoBox.POOL_ID_TAG, poolId);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
