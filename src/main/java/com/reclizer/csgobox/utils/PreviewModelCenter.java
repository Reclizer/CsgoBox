package com.reclizer.csgobox.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Vector3f;
import java.util.Map;
import java.util.WeakHashMap;

/** GUI-space centre, including the resource model's own translation, rotation and scale. */
final class PreviewModelCenter {
    record Center(float x, float y, float z) {}
    private static final Center ORIGIN = new Center(0, 0, 0);
    // New resource bakes get a new key; old models can be collected after a pack reload.
    private static final Map<BakedModel, Center> CACHE = new WeakHashMap<>();
    private PreviewModelCenter() {}

    static Center of(BakedModel model, ItemStack stack) {
        return CACHE.computeIfAbsent(model, key -> measure(key, stack));
    }

    private static Center measure(BakedModel model, ItemStack stack) {
        var pose = new PoseStack();
        model = ForgeHooksClient.handleCameraTransforms(pose, model, ItemDisplayContext.GUI, false);
        pose.translate(-.5F, -.5F, -.5F);
        var matrix = pose.last().pose();
        var min = new Vector3f(Float.POSITIVE_INFINITY);
        var max = new Vector3f(Float.NEGATIVE_INFINITY);
        var vertex = new Vector3f();
        var random = RandomSource.create();
        for (var pass : model.getRenderPasses(stack, true)) {
            // ItemRenderer draws six culled faces and the unculled face list with a fixed seed.
            for (int face = 0; face <= Direction.values().length; face++) {
                random.setSeed(42);
                Direction direction = face == Direction.values().length ? null : Direction.values()[face];
                for (var quad : pass.getQuads(null, direction, random)) {
                    int[] data = quad.getVertices();
                    int stride = data.length / 4;
                    if (stride < 3) continue;
                    for (int i = 0; i < 4; i++) {
                        int at = i * stride;
                        vertex.set(Float.intBitsToFloat(data[at]), Float.intBitsToFloat(data[at+1]), Float.intBitsToFloat(data[at+2]));
                        matrix.transformPosition(vertex);
                        if (!vertex.isFinite()) continue;
                        min.min(vertex); max.max(vertex);
                    }
                }
            }
        }
        return min.isFinite() && max.isFinite()
                ? new Center((min.x+max.x)/2, (min.y+max.y)/2, (min.z+max.z)/2) : ORIGIN;
    }
}
