package com.reclizer.csgobox.server;

import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;

public final class EditorDeleteBox {
    private EditorDeleteBox() {}
    /** Delete only the exact version the operator loaded in this editor. */
    public static String delete(ServerPlayer player, int containerId, int requestId, String id, String revision) throws IOException {
        if (!player.hasPermissions(2) || !player.isAlive()
                || !(player.containerMenu instanceof CsgoBoxCraftMenu menu)
                || menu.containerId != containerId || !menu.stillValid(player)) return "editor.csgobox.delete_denied";
        if (!menu.acceptDeleteRequest(requestId)) return "editor.csgobox.delete_duplicate";
        CsgoBoxManage.deleteDefinition(id, revision);
        ConfiguredAcquisition.refresh(player.server);
        BoxService.clearSessions();
        player.server.getPlayerList().getPlayers().forEach(BoxService::syncCatalog);
        return "";
    }
}
