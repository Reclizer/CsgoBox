package com.reclizer.csgobox.gui.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import java.io.IOException;

/** Filter this high-resolution badge only; pixel-art item textures keep their existing sampling. */
final class LegendaryIconTexture extends SimpleTexture {
    static final ResourceLocation LOCATION = new ResourceLocation("csgobox", "textures/screens/gold_item.png");

    private LegendaryIconTexture() { super(LOCATION); }

    static void ensureLoaded() {
        var textures = Minecraft.getInstance().getTextureManager();
        if (!(textures.getTexture(LOCATION, null) instanceof LegendaryIconTexture))
            textures.register(LOCATION, new LegendaryIconTexture());
    }

    @Override public void load(ResourceManager resources) throws IOException {
        super.load(resources);
        // SimpleTexture may queue its upload during a resource reload. Queue after that upload.
        if (RenderSystem.isOnRenderThreadOrInit()) prepareMipmaps();
        else RenderSystem.recordRenderCall(this::prepareMipmaps);
    }

    private void prepareMipmaps() {
        RenderSystem.assertOnRenderThreadOrInit();
        bind();
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width <= 0 || height <= 0) return;
        int levels = 31 - Integer.numberOfLeadingZeros(Math.max(width, height));
        // SimpleTexture allocates level zero and limits MAX_LEVEL to zero by default.
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, levels);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        setFilter(true, true);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }
}
