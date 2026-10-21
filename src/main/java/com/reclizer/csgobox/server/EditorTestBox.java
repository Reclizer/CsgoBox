package com.reclizer.csgobox.server;

import com.reclizer.csgobox.config.BoxConfigStore;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class EditorTestBox {
    private EditorTestBox() {}

    /** Returns a translation key, or an empty string on success. */
    public static String give(ServerPlayer player, int containerId, int requestId, String name, String revision) {
        if (!player.hasPermissions(2) || !player.isAlive()
                || !(player.containerMenu instanceof CsgoBoxCraftMenu menu)
                || menu.containerId != containerId || !menu.stillValid(player)) return "editor.csgobox.test_denied";
        var box = CsgoBoxManage.find(name);
        if (box == null) return "message.csgobox.unknown_box";
        if (!BoxConfigStore.revision(box).equals(revision)) return "editor.csgobox.test_stale";
        if (!menu.acceptTestRequest(requestId)) return "editor.csgobox.test_duplicate";
        ItemStack key = ItemNBT.parseStrict(box.key());
        BoxService.deliver(player, ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get())));
        BoxService.deliver(player, key);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return "";
    }
}
