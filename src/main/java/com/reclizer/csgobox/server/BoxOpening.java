package com.reclizer.csgobox.server;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.utils.ItemNBT;
import com.reclizer.csgobox.utils.WeightedSelection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.random.RandomGenerator;

/** Inventory transaction, independent of packet handling and animation. */
public final class BoxOpening {
    public record Result(ItemStack reward, int grade, String error) {
        public boolean success() { return error == null; }
    }
    private BoxOpening() {}

    public static Result open(ServerPlayer player, BoxDefinition box, RandomGenerator random) {
        ItemStack held = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || held.isEmpty() || !box.id().equals(ItemCsgoBox.boxId(held)))
            return new Result(ItemStack.EMPTY, 0, "message.csgobox.expired");
        ItemStack key = ItemNBT.parseStrict(box.key());
        int keySlot = -1;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (i != player.getInventory().selected && !candidate.isEmpty() && candidate.is(key.getItem())) {
                keySlot = i;
                break;
            }
        }
        if (keySlot < 0) return new Result(ItemStack.EMPTY, 0, "message.csgobox.missing_key");
        int grade = WeightedSelection.grade(random, box.random(), g -> !box.items(g).isEmpty(), box.effectiveLuck(player.getLuck()));
        List<String> pool = box.items(grade);
        ItemStack reward = ItemNBT.parseStrict(pool.get(random.nextInt(pool.size())));
        // Nothing is consumed until configuration, key and reward have all been validated.
        held.shrink(1);
        player.getInventory().getItem(keySlot).shrink(1);
        BoxService.deliver(player, reward.copy());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new Result(reward, grade, null);
    }
}
