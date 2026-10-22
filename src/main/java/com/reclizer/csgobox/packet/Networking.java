package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public final class Networking {
    private static final String PROTOCOL = "9";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CsgoBox.MODID, "network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void registerMessages() {
        INSTANCE.registerMessage(0, PacketCsgoProgress.class, PacketCsgoProgress::toBytes,
                PacketCsgoProgress::new, PacketCsgoProgress::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(1, PacketSaveBox.class, PacketSaveBox::toBytes,
                PacketSaveBox::new, PacketSaveBox::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(2, PacketBoxPreview.class, PacketBoxPreview::toBytes,
                PacketBoxPreview::new, PacketBoxPreview::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(3, PacketBoxResult.class, PacketBoxResult::toBytes,
                PacketBoxResult::new, PacketBoxResult::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(4, PacketBoxCatalog.class, PacketBoxCatalog::toBytes,
                PacketBoxCatalog::new, PacketBoxCatalog::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(5, PacketLoadBox.class, PacketLoadBox::toBytes,
                PacketLoadBox::new, PacketLoadBox::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(6, PacketEditorState.class, PacketEditorState::toBytes,
                PacketEditorState::new, PacketEditorState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(7, PacketTestBox.class, PacketTestBox::toBytes,
                PacketTestBox::new, PacketTestBox::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(8, PacketBoxRewardNames.class, PacketBoxRewardNames::toBytes,
                PacketBoxRewardNames::new, PacketBoxRewardNames::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(9, PacketPreviewHeartbeat.class, PacketPreviewHeartbeat::toBytes,
                PacketPreviewHeartbeat::new, PacketPreviewHeartbeat::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayerClient(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
