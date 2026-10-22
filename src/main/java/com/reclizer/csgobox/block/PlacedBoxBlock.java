package com.reclizer.csgobox.block;

import com.reclizer.csgobox.server.PlacedBoxOpening;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

public final class PlacedBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BoxAppearance> APPEARANCE = EnumProperty.create("appearance", BoxAppearance.class);
    private static final Map<BoxAppearance, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(BoxAppearance.class);
    static {
        for (var appearance : BoxAppearance.values()) {
            double front = appearance.front(), back = appearance.back(), height = appearance.height();
            SHAPES.put(appearance, Map.of(
                    Direction.NORTH, Block.box(0, 0, front, 16, height, back),
                    Direction.EAST, Block.box(16-back, 0, 0, 16-front, height, 16),
                    Direction.SOUTH, Block.box(0, 0, 16-back, 16, height, 16-front),
                    Direction.WEST, Block.box(front, 0, 0, back, height, 16)));
        }
    }
    private static final Random RANDOM = new Random();

    public PlacedBoxBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F).sound(SoundType.METAL)
                .noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(APPEARANCE, BoxAppearance.CLASSIC));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlacedBoxEntity(pos, state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, APPEARANCE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        String id = ItemCsgoBox.boxId(context.getItemInHand());
        BoxDefinition definition = context.getLevel().isClientSide ? null : CsgoBoxManage.find(id);
        BoxAppearance appearance = context.getLevel().isClientSide ? BoxCatalog.appearance(id)
                : definition == null ? BoxAppearance.CLASSIC : BoxAppearance.byId(definition.appearance());
        if (context.getItemInHand().getItem() instanceof EmptyBoxItem) appearance=EmptyBoxItem.appearance(context.getItemInHand());
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(APPEARANCE, appearance);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.BOX_ENTITY.get(), PlacedBoxEntity::serverTick);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(APPEARANCE)).get(state.getValue(FACING));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack stack = entity instanceof PlacedBoxEntity box ? box.asItem() : ItemStack.EMPTY;
        return stack.isEmpty() ? List.of() : List.of(stack);
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof PlacedBoxEntity box ? box.asItem() : ItemStack.EMPTY;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        var result = PlacedBoxOpening.open(serverPlayer, pos, hand, RANDOM);
        if (!result.success()) player.displayClientMessage(PlacedBoxOpening.errorMessage(serverPlayer,pos,result.error()),true);
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof PlacedBoxEntity box) box.onRemoved();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state,Level level,BlockPos pos,Player player,boolean willHarvest,FluidState fluid) {
        // Creative skips the normal loot path. A paid case still leaves its decorative shell.
        ItemStack shell=!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof PlacedBoxEntity box && box.isBusy()
                ?box.asItem():ItemStack.EMPTY;
        boolean removed=super.onDestroyedByPlayer(state,level,pos,player,willHarvest,fluid);
        if (removed && !shell.isEmpty()) Block.popResource(level,pos,shell);
        return removed;
    }
}
