package com.reclizer.csgobox.gui.client;

import java.lang.invoke.*;
import java.util.function.BooleanSupplier;

/** Uses the optional Iris/Oculus public API; neither mod is required or bundled. */
final class GroundShaderCompatibility {
    private static final BooleanSupplier SHADOW_PASS=findBooleanQuery("isRenderingShadowPass");
    private static final BooleanSupplier SHADER_PACK=findBooleanQuery("isShaderPackInUse");
    private GroundShaderCompatibility() {}
    static boolean shadowPass() { return SHADOW_PASS.getAsBoolean(); }
    static boolean shaderPackInUse() { return SHADER_PACK.getAsBoolean(); }
    private static BooleanSupplier findBooleanQuery(String name) {
        try {
            var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            var instance=api.getMethod("getInstance").invoke(null);
            var method=MethodHandles.publicLookup().unreflect(api.getMethod(name)).bindTo(instance);
            return () -> {
                try { return (boolean)method.invokeExact(); }
                catch (Throwable error) { return false; }
            };
        } catch (ReflectiveOperationException | LinkageError absent) { return () -> false; }
    }
}
