package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

/** Optional, isolated visual fixture; never included in the release JAR. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID,value=Dist.CLIENT)
public final class GroundVisualClientChecks {
    private static long start=-1, wallStart;
    private static int warmup, next, frames;
    private static boolean done;
    private static final int[] TIMES={120,300,340,440,500};
    private static final String[] NAMES={"closed","spinning","night-spinning","open-after-reward","reset"};
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        if (done || !Boolean.getBoolean("csgobox.groundVisualChecks") || event.phase!=TickEvent.Phase.END
                || mc.level==null || mc.player==null || ++warmup<40) return;
        try {
            if (start<0) {
                if (Boolean.getBoolean("csgobox.groundVisualGpuChecks")) GroundRenderClientChecks.run();
                start=mc.level.getGameTime(); wallStart=System.nanoTime(); mc.options.hideGui=true;
                beginDemo(mc);
            }
            frames++;
            long elapsed=mc.level.getGameTime()-start;
            if (next<TIMES.length && elapsed>=TIMES[next]) {
                var dir=Path.of(System.getProperty("csgobox.groundVisualOutput","screenshots/ground-visual"));
                Files.createDirectories(dir);
                var path=dir.resolve(NAMES[next++]+".png");
                try (var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
                CsgoBox.LOGGER.info("Ground visual capture: {} (tick {})",path.toAbsolutePath(),elapsed);
                if (Boolean.getBoolean("csgobox.groundVisualLighting") && (next==2 || next==3)) {
                    long time=next==2?18000:6000;
                    var server=mc.getSingleplayerServer();
                    server.execute(()->server.overworld().setDayTime(time));
                }
            }
            if (elapsed>540) {
                done=true;
                CsgoBox.LOGGER.info("GROUND VISUAL SCENARIO COMPLETE: {} frames, {} seconds",frames,(System.nanoTime()-wallStart)/1e9);
                mc.stop();
            }
        } catch (Exception error) { CsgoBox.LOGGER.error("GROUND VISUAL SCENARIO FAILED",error); mc.stop(); }
    }
    static void beginDemo(Minecraft mc) {
        var server=mc.getSingleplayerServer(); var id=mc.player.getUUID();
        server.execute(()-> {
            var player=server.getPlayerList().getPlayer(id);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4),"csgobox_test_ground demo");
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(63,-61,186,70,-55,195)).forEach(net.minecraft.world.entity.Entity::discard);
            player.connection.teleport(66.5,-60,188.4,0,38);
        });
    }
}
