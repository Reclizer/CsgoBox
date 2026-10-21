package com.reclizer.csgobox.gui;

import com.reclizer.csgobox.config.RewardEntry;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import java.util.ArrayList;
import java.util.List;

public class CsgoBoxCraftMenu extends AbstractContainerMenu {
    public static final int REWARD_SLOTS = 35;
    private final ItemStackHandler rewards = new ItemStackHandler(REWARD_SLOTS);

    public CsgoBoxCraftMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        super(RecModMenus.CSGO_BOX_CRAFT.get(), id);
        int index = 0;
        int[] widths = {7, 5, 3, 2, 9, 9};
        int[] rows = {-20, -2, 16, 34, 70, 88};
        for (int row = 0; row < widths.length; row++) {
            for (int column = 0; column < widths[row]; column++)
                addSlot(new SlotItemHandler(rewards, index++, 20 + column * 18, rows[row]));
        }
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + (row + 1) * 9, 20 + column * 18, 121 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 20 + column * 18, 179));
    }

    public List<RewardEntry> rewards() {
        List<RewardEntry> result = new ArrayList<>();
        for (int slot = 0; slot < REWARD_SLOTS; slot++) {
            ItemStack stack = rewards.getStackInSlot(slot);
            if (!stack.isEmpty()) result.add(new RewardEntry(ItemNBT.getStacksData(stack), gradeForSlot(slot)));
        }
        return List.copyOf(result);
    }

    public static int gradeForSlot(int slot) {
        if (slot < 0 || slot >= REWARD_SLOTS) throw new IllegalArgumentException("Invalid reward slot");
        return slot < 7 ? 1 : slot < 12 ? 2 : slot < 15 ? 3 : slot < 17 ? 4 : 5;
    }

    @Override
    public boolean stillValid(Player player) { return player.isAlive() && player.hasPermissions(2); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        boolean moved = index < REWARD_SLOTS
                ? moveItemStackTo(source, REWARD_SLOTS, slots.size(), true)
                : moveItemStackTo(source, 0, REWARD_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        for (int slot = 0; slot < REWARD_SLOTS; slot++) {
            ItemStack stack = rewards.extractItem(slot, rewards.getStackInSlot(slot).getCount(), false);
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) player.drop(stack, false);
            else player.getInventory().placeItemBackInInventory(stack);
        }
    }
}
