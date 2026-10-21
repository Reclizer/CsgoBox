package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.compat.TaczCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.Optional;

/** Resolve optional gun metadata once per screen, never from the animation render loop. */
record CardItemLayout(float widthFraction, float heightFraction) {
    float size(float width, float height) { return Math.min(width * widthFraction, height * heightFraction); }

    static CardItemLayout of(ItemStack stack) {
        if (TaczCompat.isGun(stack)) {
            boolean pistol = false;
            try {
                var api = Class.forName("com.tacz.guns.api.TimelessAPI");
                var index = (Optional<?>) api.getMethod("getCommonGunIndex", ResourceLocation.class)
                        .invoke(null, ResourceLocation.tryParse(TaczCompat.gunId(stack)));
                if (index.isPresent()) {
                    String type = String.valueOf(index.get().getClass().getMethod("getType").invoke(index.get()));
                    pistol = type.equalsIgnoreCase("pistol") || type.equalsIgnoreCase("handgun");
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Other TaCZ API versions can still display their ordinary GUI gun icon.
            }
            return pistol ? new CardItemLayout(0.78F, 0.88F) : new CardItemLayout(0.90F, 1.14F);
        }
        var item = stack.getItem();
        if (item instanceof SwordItem || item instanceof DiggerItem || item instanceof TridentItem
                || item instanceof BowItem || item instanceof CrossbowItem) return new CardItemLayout(0.80F, 0.85F);
        return new CardItemLayout(0.78F, 0.80F);
    }
}
