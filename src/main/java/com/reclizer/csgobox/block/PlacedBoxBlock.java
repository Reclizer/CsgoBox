package com.reclizer.csgobox.block;

import com.reclizer.csgobox.server.PlacedBoxOpening;
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
    private static final VoxelShape NORTH_SOUTH = Block.box(0, 0, 1, 16, 9, 14);
    private static final VoxelShape EAST_WEST = Block.box(1, 0, 0, 14, 9, 16);
    private static final Random RANDOM = new Random();

    public PlacedBoxBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F).sound(SoundType.METAL)
                .noOcclusion().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlacedBoxEntity(pos, state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? EAST_WEST : NORTH_SOUTH;
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
        if (!result.success()) player.displayClientMessage(Component.translatable(result.error()), true);
        else {
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, .8F, 1.05F);
            player.displayClientMessage(Component.translatable("message.csgobox.ground_reward",
                    result.reward().getHoverName(), result.reward().getCount()), true);
        }
        return InteractionResult.CONSUME;
    }
}
