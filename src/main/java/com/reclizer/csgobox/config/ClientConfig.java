package com.reclizer.csgobox.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Forge loads and watches config/csgobox-client.toml on the physical client. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BLUR;
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_OPACITY;
    public static final ForgeConfigSpec.BooleanValue TACZ_3D_PREVIEW;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("display");
        BLUR = builder.comment("Enable the opening-screen blur. Disable if another mod or shader causes flickering.",
                "开箱背景模糊；遇到光影或其他模组闪烁时可关闭。")
                .define("blurEnabled", true);
        BACKGROUND_OPACITY = builder.comment("Opening-screen background opacity. 0 = transparent, 1 = opaque.",
                "开箱背景不透明度：0 为透明，1 为不透明。")
                .defineInRange("backgroundOpacity", 0.5D, 0D, 1D);
        TACZ_3D_PREVIEW = builder.comment("Render TaCZ guns as 3D models on the reward inspection screen.",
                "中奖检视使用 TaCZ 三维枪械模型；关闭后使用其背包图标。")
                .define("tacz3dPreview", true);
        builder.pop();
        SPEC = builder.build();
    }
    private ClientConfig() {}
}
