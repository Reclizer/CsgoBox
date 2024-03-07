package com.reclizer.csgobox.packet;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Networking {
    public static SimpleChannel INSTANCE;

    private static int ID = 0;

    public static int nextID() {
        return ID++;
    }

    public static void registerMessages() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(CsgoBox.MODID, "network"), () -> "1.0", s -> true, s -> true);




        INSTANCE.registerMessage(nextID(),
                PacketCsgoProgress.class,
                PacketCsgoProgress::toBytes,
                PacketCsgoProgress::new,
                PacketCsgoProgress::handle);

        INSTANCE.registerMessage(nextID(),
                PacketUpdateMode.class,
                PacketUpdateMode::toBytes,
                PacketUpdateMode::new,
                PacketUpdateMode::handle);



        INSTANCE.registerMessage(nextID(),
                PacketGiveItem.class,
                PacketGiveItem::toBytes,
                PacketGiveItem::new,
                PacketGiveItem::handle);

        INSTANCE.registerMessage(nextID(),
                PacketLookItem.class,
                PacketLookItem::toBytes,
                PacketLookItem::new,
                PacketLookItem::handle);



    }






    public static void sendToNearby(Level world, BlockPos pos, Object toSend) {
        if (world instanceof ServerLevel ws) {
            ws.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64)
                    .forEach(p -> INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), toSend));
        }
    }

    public static void sendToNearby(Level world, Entity e, Object toSend) {
        sendToNearby(world, e.blockPosition(), toSend);
    }

    public static void sendToPlayerClient(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }
}
