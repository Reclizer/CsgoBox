package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

/** Actual Oculus on -> off -> on transitions in one isolated client. Excluded from releases. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID,value=Dist.CLIENT)
public final class GroundShaderToggleClientChecks {
    private static int warmup, phase, waitFrames;
    private static long started=-1;
    private static boolean captured, done;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        if (done || !Boolean.getBoolean("csgobox.groundToggleChecks") || event.phase!=TickEvent.Phase.END
                || mc.level==null || mc.player==null || ++warmup<40) return;
        try {
            if (started<0) {
                if (++waitFrames<30) return;
                var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                boolean active=(boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));
                if (active!=(phase!=1)) throw new AssertionError("Incorrect shader state in toggle phase "+phase+": "+active);
                GroundRenderClientChecks.run();
                if (phase>0) capture(mc,phase==1?"toggle-off-live":"toggle-on-live");
                mc.options.hideGui=true; GroundVisualClientChecks.beginDemo(mc);
                started=mc.level.getGameTime(); captured=false;
                CsgoBox.LOGGER.info("Ground shader toggle phase {} started: shaders={}",phase,active);
            }
            long elapsed=mc.level.getGameTime()-started;
            if (!captured && elapsed>=300) {
                capture(mc,new String[]{"shader-on","shader-off","shader-on-again"}[phase]); captured=true;
            }
            if (phase<2 && elapsed>=360) {
                var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                var config=api.getMethod("getConfig").invoke(api.getMethod("getInstance").invoke(null));
                Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply",boolean.class)
                        .invoke(config,phase==1);
                phase++; started=-1; waitFrames=0;
            } else if (phase==2 && elapsed>=600) {
                done=true;
                CsgoBox.LOGGER.info("GROUND SHADER TOGGLE CHECKS PASSED: on -> off -> on in one client, real panel pixels and world captures in every phase");
                mc.stop();
            }
        } catch (Throwable error) { done=true; CsgoBox.LOGGER.error("GROUND SHADER TOGGLE CHECKS FAILED",error); mc.stop(); }
    }
    private static void capture(Minecraft mc,String name) throws Exception {
        var directory=Path.of(System.getProperty("csgobox.groundToggleOutput","screenshots/ground-toggle"));
        Files.createDirectories(directory); var path=directory.resolve(name+".png");
        try (var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
        CsgoBox.LOGGER.info("Ground shader toggle capture: {}",path.toAbsolutePath());
    }
}
