package com.reclizer.csgobox.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Read-only inventory samples. Editing never transfers, consumes or creates inventory items. */
public class CsgoBoxCraftMenu extends AbstractContainerMenu {
    public CsgoBoxCraftMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        super(RecModMenus.CSGO_BOX_CRAFT.get(), id);
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                sample(inventory, column + (row + 1) * 9, 152 + column * 20, 306 + row * 20);
        for (int column = 0; column < 9; column++) sample(inventory, column, 152 + column * 20, 370);
    }

    private void sample(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
    }

    @Override public boolean stillValid(Player player) { return player.isAlive() && player.hasPermissions(2); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {}
}
