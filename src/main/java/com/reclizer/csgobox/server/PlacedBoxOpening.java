package com.reclizer.csgobox.server;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.utils.BoxKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.random.RandomGenerator;

/** Validate and charge once, then let the durable block animation release its fixed result. */
public final class PlacedBoxOpening {
    private PlacedBoxOpening() {}
    private static BoxOpening.Result fail(String key) { return new BoxOpening.Result(ItemStack.EMPTY, 0, key); }

    public static BoxOpening.Result open(ServerPlayer player, BlockPos pos, InteractionHand hand, RandomGenerator random) {
        var level = player.serverLevel();
        if (!player.isAlive() || player.isSpectator() || !level.isLoaded(pos) || !player.canReach(pos, 1)
                || !level.mayInteract(player, pos) || !player.mayInteract(level, pos)
                || !level.getBlockState(pos).is(ModBlocks.BOX.get())
                || !(level.getBlockEntity(pos) instanceof PlacedBoxEntity entity))
            return fail("message.csgobox.placed_unavailable");
        if (entity.isBusy()) return fail("message.csgobox.ground_busy");
        if (entity.isEmpty()) { entity.toggleEmpty(); return new BoxOpening.Result(ItemStack.EMPTY,0,null); }
        var box = CsgoBoxManage.find(entity.poolId());
        if (box == null) return fail("message.csgobox.unknown_box");
        var mode = box.mode();
        if (mode.requiresKey() && box.key().isEmpty()) return fail("message.csgobox.hold_key");
        // A configured ground key unlocks BOX mode too, but that mode does not consume it.
        ItemStack required = box.key().isEmpty() ? ItemStack.EMPTY : BoxKeys.parse(box.key());
        ItemStack key = player.getItemInHand(hand);
        if (!required.isEmpty() && !BoxKeys.matches(required, key)) {
            // Vanilla normally attempts main hand first. A held offhand key works as well.
            key = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!BoxKeys.matches(required, key)) return fail("message.csgobox.hold_key");
        }
        BoxOpening.Result result = BoxOpening.selectReward(player, box, random);
        if (!entity.begin(box,result,player.getUUID(),random.nextLong())) return fail("message.csgobox.placed_unavailable");
        if (mode.consumesKey() && !required.isEmpty()) key.shrink(1);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        level.gameEvent(player,GameEvent.BLOCK_OPEN,pos);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.CHEST_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,.8F,1.05F);
        return new BoxOpening.Result(ItemStack.EMPTY,0,null);
    }
    public static net.minecraft.network.chat.Component errorMessage(ServerPlayer player,BlockPos pos,String error) {
        if ("message.csgobox.hold_key".equals(error)
                && player.level().getBlockEntity(pos) instanceof PlacedBoxEntity entity) {
            var box=CsgoBoxManage.find(entity.poolId());
            if (box!=null && !box.key().isBlank()) return net.minecraft.network.chat.Component.translatable(
                    "message.csgobox.hold_key_named",BoxKeys.parse(box.key()).getHoverName());
        }
        return net.minecraft.network.chat.Component.translatable(error);
    }
}
