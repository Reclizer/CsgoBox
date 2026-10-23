package com.reclizer.csgobox.gametest;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.utils.GroundAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;
import org.lwjgl.opengl.*;
import java.lang.reflect.*;
import java.util.*;

/** Real-GL regression checks on private test targets, not screenshots. Excluded from release JARs. */
@Mod.EventBusSubscriber(modid=CsgoBox.MODID,value=Dist.CLIENT)
public final class GroundRenderClientChecks {
    private static boolean checked;
    private static int frames;
    private static boolean shaderScenarioStarted;
    private static long shaderScenarioTick;
    private static final Set<com.reclizer.csgobox.config.BoxAppearance> renderedAppearances=
            EnumSet.noneOf(com.reclizer.csgobox.config.BoxAppearance.class);
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        if (!Boolean.getBoolean("csgobox.groundRenderChecks") || checked || event.phase!=TickEvent.Phase.END
                || mc.level==null || mc.player==null || ++frames<40) return;
        if (Boolean.getBoolean("csgobox.groundShaderChecks")) {
            try { shaderScenario(mc); }
            catch (Throwable error) { checked=true; CsgoBox.LOGGER.error("GROUND SHADER COMPAT CHECKS FAILED",error); mc.stop(); }
            return;
        }
        checked=true;
        try { run(); } catch (Throwable error) { CsgoBox.LOGGER.error("GROUND GUI GPU CHECKS FAILED",error); }
        finally { if (Boolean.getBoolean("csgobox.groundChecksExit")) mc.stop(); }
    }
    private static void shaderScenario(Minecraft mc) throws Exception {
        if (!shaderScenarioStarted) {
            shaderScenarioStarted=true;
            var iris=Class.forName("net.irisshaders.iris.Iris");
            if (!Boolean.TRUE.equals(iris.getMethod("isPackInUseQuick").invoke(null)))
                throw new AssertionError("The shader compatibility test requires an active shader pack");
            CsgoBox.LOGGER.info("Ground shader test pack: {}",iris.getMethod("getCurrentPackName").invoke(null));
            run();
            shaderScenarioTick=mc.level.getGameTime();
            var server=Objects.requireNonNull(mc.getSingleplayerServer());
            var playerId=mc.player.getUUID();
            server.execute(()-> {
                var player=Objects.requireNonNull(server.getPlayerList().getPlayer(playerId));
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4),"csgobox_test_ground demo");
            });
        }
        var field=Class.forName("com.reclizer.csgobox.gui.client.GroundReelTexture").getDeclaredField("CACHE");
        field.setAccessible(true);
        for (var key:((Map<?,?>)field.get(null)).keySet()) {
            if (key instanceof PlacedBoxEntity box && box.getLevel()==mc.level && !box.getBlockPos().equals(BlockPos.ZERO))
                renderedAppearances.add(box.getBlockState().getValue(PlacedBoxBlock.APPEARANCE));
        }
        if (mc.level.getGameTime()-shaderScenarioTick>800) {
            if (renderedAppearances.size()!=com.reclizer.csgobox.config.BoxAppearance.values().length)
                throw new AssertionError("Not all ground reels were rendered with shaders: "+renderedAppearances);
            checked=true;
            CsgoBox.LOGGER.info("GROUND SHADER COMPAT CHECKS PASSED: active shader pack, all five world case reels rendered and completed: {}",renderedAppearances);
            if (Boolean.getBoolean("csgobox.groundChecksExit")) mc.stop();
        }
    }
    private static AutoCloseable guard() throws Exception {
        var constructor=Class.forName("com.reclizer.csgobox.gui.client.WorldGuiRenderState").getDeclaredConstructor();
        constructor.setAccessible(true); return (AutoCloseable)constructor.newInstance();
    }
    private static List<Integer> ints(int... values) { return Arrays.stream(values).boxed().toList(); }
    private static List<Float> floats(float[] values) {
        var list=new ArrayList<Float>(); for (float v:values) list.add(v); return list;
    }
    private static Map<String,Object> state() {
        var result=new LinkedHashMap<String,Object>();
        result.put("lights",List.of(new Vector3f(RenderSystem.shaderLightDirections[0]),new Vector3f(RenderSystem.shaderLightDirections[1])));
        result.put("shader",RenderSystem.getShader()); result.put("color",floats(RenderSystem.getShaderColor().clone()));
        result.put("fog",List.of(RenderSystem.getShaderFogStart(),RenderSystem.getShaderFogEnd(),RenderSystem.getShaderFogShape(),floats(RenderSystem.getShaderFogColor().clone())));
        result.put("matrices",List.of(new Matrix4f(RenderSystem.getProjectionMatrix()),new Matrix4f(RenderSystem.getTextureMatrix()),
                new Matrix4f(RenderSystem.getModelViewStack().last().pose()),new Matrix3f(RenderSystem.getModelViewStack().last().normal()),RenderSystem.getVertexSorting()));
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE); var bindings=new int[3];
        for (int i=0;i<3;i++) { RenderSystem.activeTexture(GL13.GL_TEXTURE0+i); bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); }
        RenderSystem.activeTexture(active); result.put("textures",ints(bindings)); result.put("activeTexture",active);
        var samplers=new int[12]; for (int i=0;i<12;i++) samplers[i]=RenderSystem.getShaderTexture(i);
        result.put("samplers",ints(samplers));
        result.put("framebuffers",ints(GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)));
        var viewport=new int[4]; var scissor=new int[4]; var clear=new float[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport); GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor); GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clear);
        result.put("viewport",ints(viewport)); result.put("scissor",List.of(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),ints(scissor)));
        result.put("clearColor",floats(clear));
        var mask=new int[4]; GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK,mask); result.put("colorMask",ints(mask));
        result.put("depth",List.of(GL11.glIsEnabled(GL11.GL_DEPTH_TEST),GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),GL11.glGetInteger(GL11.GL_DEPTH_FUNC)));
        result.put("blend",List.of(GL11.glIsEnabled(GL11.GL_BLEND),GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)));
        result.put("cull",GL11.glIsEnabled(GL11.GL_CULL_FACE)); return result;
    }
    private static void unchanged(Map<String,Object> before) {
        var after=state();
        for (var entry:before.entrySet()) if (!Objects.equals(entry.getValue(),after.get(entry.getKey())))
            throw new AssertionError("World render state leaked: "+entry.getKey()+"; before="+entry.getValue()+"; after="+after.get(entry.getKey()));
    }
    private static PlacedBoxEntity box(ItemStack item,long seed) {
        var mc=Minecraft.getInstance(); var box=new PlacedBoxEntity(BlockPos.ZERO,ModBlocks.BOX.get().defaultBlockState()); box.setLevel(mc.level);
        var tag=new CompoundTag(); tag.putString("Phase","opening"); tag.putLong("OpeningStarted",mc.level.getGameTime()-GroundAnimation.LID_TICKS-50);
        tag.putLong("VisualSeed",seed); tag.put("PendingReward",item.save(new CompoundTag())); tag.putInt("Grade",3);
        var samples=new ListTag(); var sample=new CompoundTag(); sample.put("Item",item.save(new CompoundTag())); sample.putInt("Grade",3); samples.add(sample);
        tag.put("ReelSamples",samples); box.load(tag); return box;
    }
    private static int[] pixels(TextureTarget target) throws Exception {
        try (var saved=guard(); var image=new NativeImage(target.width,target.height,false)) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0); RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0,false); return image.getPixelsRGBA();
        }
    }
    private static void missingLightUniforms(Method render) throws Exception {
        var shader=GameRenderer.getRendertypeEntityCutoutShader();
        var first=ShaderInstance.class.getField("LIGHT0_DIRECTION");
        var second=ShaderInstance.class.getField("LIGHT1_DIRECTION");
        first.setAccessible(true); second.setAccessible(true);
        var originalFirst=first.get(shader); var originalSecond=second.get(shader);
        try {
            // Exercise either missing uniform and both missing, as in shader-pack replacements.
            for (int mask=1;mask<=3;mask++) {
                first.set(shader,(mask&1)!=0?null:originalFirst);
                second.set(shader,(mask&2)!=0?null:originalSecond);
                RenderSystem.setShaderLights(new Vector3f(.31F,.72F,-.51F),new Vector3f(-.42F,.61F,.27F));
                var before=state();
                render.invoke(null,box(new ItemStack(Items.DIAMOND_SWORD),91+mask),0F,new Matrix4f());
                unchanged(before);
            }
        } finally { first.set(shader,originalFirst); second.set(shader,originalSecond); }
        CsgoBox.LOGGER.info("GROUND MISSING-UNIFORM CHECKS PASSED: either/both absent, complete reel renders, exact world-state restoration");
    }
    private static void run() throws Exception {
        // Separate shader-pack startup/driver errors from the operations checked below.
        for (int error;(error=GL11.glGetError())!=GL11.GL_NO_ERROR;)
            CsgoBox.LOGGER.warn("Ground GUI checks cleared a pre-existing OpenGL error: {}",error);
        var renderer=Class.forName("com.reclizer.csgobox.gui.client.GroundReelTexture");
        var render=renderer.getDeclaredMethod("render",PlacedBoxEntity.class,float.class,Matrix4f.class); render.setAccessible(true);
        var field=renderer.getDeclaredField("target"); field.setAccessible(true);
        TextureTarget scratch=null;
        try (var saved=guard()) {
            scratch=new TextureTarget(64,64,true,Minecraft.ON_OSX); scratch.bindWrite(true);
            var leather=new ItemStack(Items.LEATHER_CHESTPLATE); leather.getOrCreateTagElement("display").putInt("color",0xE01010);
            var items=List.of(new ItemStack(Items.DIAMOND_SWORD),leather,new ItemStack(Items.STONE));
            for (int i=0;i<items.size();i++) {
                var box=box(items.get(i),73+i);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.setShaderLights(new Vector3f(.17F,.83F,-.41F),new Vector3f(-.23F,-.57F,.69F));
                RenderSystem.setShaderFogStart(2); RenderSystem.setShaderFogEnd(12); RenderSystem.setShaderFogColor(.15F,.45F,.7F,1);
                RenderSystem.setShaderColor(.4F,.7F,.9F,.6F); RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
                RenderSystem.enableBlend(); RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
                RenderSystem.enableScissor(4,5,20,25); RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                RenderSystem.colorMask(true,false,true,false);
                var before=state(); render.invoke(null,box,0F,new Matrix4f()); unchanged(before);
                int[] foggy=pixels((TextureTarget)field.get(null));
                RenderSystem.setShaderFogStart(Float.MAX_VALUE); RenderSystem.setShaderColor(1,1,1,1);
                RenderSystem.setShaderLights(new Vector3f(1,0,0),new Vector3f(0,1,0));
                before=state(); render.invoke(null,box,0F,new Matrix4f()); unchanged(before);
                int[] clear=pixels((TextureTarget)field.get(null));
                if (!Arrays.equals(foggy,clear)) throw new AssertionError("GUI colors depend on world fog/light for "+items.get(i));
                long colored=Arrays.stream(clear).filter(p->{int r=p&255,g=p>>8&255,b=p>>16&255;
                    return r>120 && r>g*1.7 && r>b*1.7;}).count();
                if (i==1 && colored<20) throw new AssertionError("Dyed leather lost its red item tint");
                long cyan=Arrays.stream(clear).filter(p->{int r=p&255,g=p>>8&255,b=p>>16&255; return g>90 && b>90 && r<g*.65 && r<b*.65;}).count();
                var histogram=Arrays.stream(clear).boxed().collect(java.util.stream.Collectors.groupingBy(p->p,java.util.stream.Collectors.counting()));
                var top=histogram.entrySet().stream().sorted(Map.Entry.<Integer,Long>comparingByValue().reversed()).limit(12)
                        .map(e->String.format("%08x=%d",e.getKey(),e.getValue())).toList();
                CsgoBox.LOGGER.info("Ground GUI pixel diagnostics: item={}, cyan={}, red={}, colors={}, histogram={}",items.get(i),cyan,colored,histogram.size(),top);
                if (i==0 && cyan<20) throw new AssertionError("Diamond sword lost its cyan texture color");
                CsgoBox.LOGGER.info("Ground GUI GPU item check passed: {}, identical colors under different fog/lights; world state restored",items.get(i));
            }
            var before=state();
            try (var nested=guard()) { RenderSystem.setShaderFogStart(-100); RenderSystem.setShaderColor(0,0,0,0); throw new Expected(); }
            catch (Expected expected) { unchanged(before); }
            missingLightUniforms(render);
            if (GL11.glGetError()!=GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error in ground GUI checks");
            CsgoBox.LOGGER.info("GROUND GUI GPU CHECKS PASSED: 3 items, 9 renders, absent light uniforms, fog/color/light isolation, exact world-state restoration, exception cleanup");
        } finally { if (scratch!=null) try (var cleanup=guard()) { scratch.destroyBuffers(); } }
    }
    private static final class Expected extends RuntimeException {}
}
