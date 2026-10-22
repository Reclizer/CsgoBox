package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.config.BoxAppearance;
import com.reclizer.csgobox.item.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Cosmetic lookup only. Real boxes continue to store just their pool identity. */
public final class BoxAppearanceModels {
    private static final String PREVIEW_TAG = "CsgoBoxEditorAppearance";
    private BoxAppearanceModels() {}
    public static void register() {
        ItemProperties.register(ModItems.ITEM_CSGOBOX.get(), new ResourceLocation("csgobox", "appearance"),
                (stack, level, entity, seed) -> appearance(stack).modelNumber() / 10F);
        ItemProperties.register(ModItems.ITEM_EMPTY_BOX.get(), new ResourceLocation("csgobox", "appearance"),
                (stack, level, entity, seed) -> EmptyBoxItem.appearance(stack).modelNumber() / 10F);
    }
    public static BoxAppearance appearance(ItemStack stack) {
        if (stack.getItem() instanceof EmptyBoxItem) return EmptyBoxItem.appearance(stack);
        if (stack.hasTag() && stack.getTag().contains(PREVIEW_TAG))
            return BoxAppearance.byId(stack.getTag().getString(PREVIEW_TAG));
        return BoxCatalog.appearance(ItemCsgoBox.boxId(stack));
    }
    /** An editor-only render sample, never sent or given to players. */
    public static ItemStack preview(BoxAppearance appearance) {
        var sample = new ItemStack(ModItems.ITEM_CSGOBOX.get());
        sample.getOrCreateTag().putString(PREVIEW_TAG, appearance.id());
        return sample;
    }
}
