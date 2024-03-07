package com.reclizer.csgobox.utils;


import com.mojang.blaze3d.shaders.Uniform;
import com.reclizer.csgobox.CsgoBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CsgoBox.MODID)
public class BlurHandler {

    /** uses `minecraft` as namespace because optifine breaks the shader loading */
    private static final ResourceLocation fade_in_blur = new ResourceLocation("shaders/post/fade_in_blur.json");
    public static boolean isShaderOn;
    private static float prevProgress = -1;
    private static long start;
    private static Field _listShaders;
    private static String lastShader;





    @SubscribeEvent
    public static void onGuiChange(ScreenEvent.Init.Post event) throws SecurityException {
        updateShader(true);
    }


    @SubscribeEvent
    public static void onGuiChange(ScreenEvent.Closing event) throws SecurityException {
//        updateShader(true);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().screen != null && Minecraft.getInstance().gameRenderer.currentEffect() != null) {
            float progress = getProgress();
            if (progress != prevProgress) {
                prevProgress = progress;
                updateUniform("Progress", progress);
            }
        }
    }


    public static void updateUniform(String name, float value) {
        if (_listShaders == null) return;
        PostChain sg = Minecraft.getInstance().gameRenderer.currentEffect();
        if(sg != null) {
            try {
                @SuppressWarnings("unchecked")
                List<PostPass> shaders = (List<PostPass>) _listShaders.get(sg);
                for (PostPass s : shaders) {
                    Uniform su = s.getEffect().getUniform(name);
                    if (su != null) {
                        su.set(value);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public static void updateShader(boolean excluded) {

        isShaderOn= !excluded;

        if (_listShaders == null) {
            _listShaders = ObfuscationReflectionHelper.findField(PostChain.class, "f_110009_");
        }
        if (Minecraft.getInstance().level != null) {
            GameRenderer er = Minecraft.getInstance().gameRenderer;
            PostChain postChain = er.currentEffect();
            if (!excluded) {
                boolean setShader = false;
                if (postChain == null) {
                    setShader = true;
                } else if (!fade_in_blur.toString().equals(postChain.getName())) {
                    setShader = true;
                    lastShader = postChain.getName();
                }
                if (setShader) {
                    er.loadEffect(fade_in_blur);
                    updateUniform("Radius", 50);
                    if (start == -1) {
                        start = System.currentTimeMillis();
                    } else {
                        updateUniform("Progress", getProgress());
                    }
                }

            } else if (postChain != null && fade_in_blur.toString().equals(postChain.getName())) {
                if (lastShader != null) {
                    er.loadEffect(new ResourceLocation(lastShader));
                    lastShader = null;
                } else {
                    er.shutdownEffect();
                }
                start = -1;
                prevProgress = -1;
            }
        } else {
            start = -1;
            prevProgress = -1;
        }
    }

    private static float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) 100, 1);
    }
    public static int getBackgroundColor(boolean second) {
        //透明度/r/g/b
        int color = (64 << 24) | (128 << 16) | (128 << 8) | 128;;
        int a = color >> 24;
        int r = (color >> 16) & 0xFF;
        int b = (color >> 8) & 0xFF;
        int g = color & 0xFF;
        float prog = getProgress();
        a *= prog;
        r *= prog;
        g *= prog;
        b *= prog;
        return a << 24 | r << 16 | b << 8 | g;
    }
}
