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

/** A server-thread transaction: claim the box, charge and deliver, then release a reusable box. */
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
        var box = CsgoBoxManage.find(entity.poolId());
        if (box == null) return fail("message.csgobox.unknown_box");
        var mode = box.mode();
        if (mode.requiresKey() && box.key().isEmpty()) return fail("message.csgobox.hold_key");
        ItemStack required = mode.requiresKey() ? BoxKeys.parse(box.key()) : ItemStack.EMPTY;
        ItemStack key = player.getItemInHand(hand);
        if (!required.isEmpty() && !BoxKeys.matches(required, key)) {
            // Vanilla normally attempts main hand first. A held offhand key works as well.
            key = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!BoxKeys.matches(required, key)) return fail("message.csgobox.hold_key");
        }
        BoxOpening.Result result = BoxOpening.selectReward(player, box, random);
        if (!entity.claim()) return fail("message.csgobox.placed_unavailable");
        if (mode.consumesBox() && !level.removeBlock(pos, false)) {
            entity.releaseClaim();
            return fail("message.csgobox.placed_unavailable");
        }
        try {
            if (!required.isEmpty()) key.shrink(1);
            BoxService.deliver(player, result.reward().copy());
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            level.gameEvent(player, mode.consumesBox() ? GameEvent.BLOCK_DESTROY : GameEvent.BLOCK_OPEN, pos);
            return result;
        } finally {
            if (!mode.consumesBox()) entity.releaseClaim();
        }
    }
}
