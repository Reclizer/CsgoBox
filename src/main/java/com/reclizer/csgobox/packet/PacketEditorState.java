package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.CsgoBoxCraftScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record PacketEditorState(int containerId, int requestId, boolean saved, String json, String revision, String error) {
    public PacketEditorState(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(BoxEditorCodec.MAX_JSON),
                buf.readUtf(64), buf.readUtf(2048));
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(requestId); buf.writeBoolean(saved);
        buf.writeUtf(json, BoxEditorCodec.MAX_JSON); buf.writeUtf(revision, 64); buf.writeUtf(error, 2048);
    }
    public static PacketEditorState success(int container, int request, boolean saved, BoxDefinition box) {
        return new PacketEditorState(container, request, saved, BoxEditorCodec.encode(box), BoxConfigStore.revision(box), "");
    }
    public static PacketEditorState failure(int container, int request, String error) {
        return new PacketEditorState(container, request, false, "", "", error == null ? "Unknown error" : error.substring(0, Math.min(2048, error.length())));
    }
    public static PacketEditorState testResult(int container, int request, String error) {
        return new PacketEditorState(container, request, false, "", "", error);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> receive(this)));
        context.setPacketHandled(true);
    }
    private static void receive(PacketEditorState packet) {
        if (Minecraft.getInstance().screen instanceof CsgoBoxCraftScreen screen) screen.receive(packet);
    }
}
