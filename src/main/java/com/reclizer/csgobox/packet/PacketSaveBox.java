package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import com.reclizer.csgobox.server.BoxService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Only operators with an open editor may submit a bounded, validated configuration draft. */
public record PacketSaveBox(int containerId, int requestId, String json, String revision) {
    public PacketSaveBox(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readUtf(BoxEditorCodec.MAX_JSON), buf.readUtf(64));
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(requestId);
        buf.writeUtf(json, BoxEditorCodec.MAX_JSON); buf.writeUtf(revision, 64);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.hasPermissions(2) || !player.isAlive()
                    || !(player.containerMenu instanceof CsgoBoxCraftMenu menu)
                    || menu.containerId != containerId || !menu.stillValid(player)) return;
            try {
                BoxDefinition box = BoxEditorCodec.decode(json);
                CsgoBoxManage.saveDefinition(box, revision);
                com.reclizer.csgobox.server.ConfiguredAcquisition.refresh(player.server);
                BoxService.clearSessions();
                player.server.getPlayerList().getPlayers().forEach(BoxService::syncCatalog);
                Networking.sendToPlayerClient(PacketEditorState.success(containerId, requestId, true, box), player);
            } catch (java.io.IOException | RuntimeException ex) {
                CsgoBox.LOGGER.warn("Cannot save editor draft", ex);
                Networking.sendToPlayerClient(PacketEditorState.failure(containerId, requestId, ex.getMessage()), player);
            }
        });
        context.setPacketHandled(true);
    }
}
