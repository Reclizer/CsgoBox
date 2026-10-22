package com.reclizer.csgobox.compat.jei;

import com.reclizer.csgobox.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Locale;

/** Read-only overview of enabled server rules. The actual workbench recipe remains vanilla. */
final class AcquisitionCategory implements IRecipeCategory<AcquisitionRecipe> {
    private final IDrawable icon, arrow;
    AcquisitionCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(ModItems.ITEM_CSGOBOX.get()); arrow = gui.getRecipeArrow();
    }
    @Override public RecipeType<AcquisitionRecipe> getRecipeType() { return CsgoBoxJeiPlugin.TYPE; }
    @Override public Component getTitle() { return tr("title"); }
    @Override public int getWidth() { return 172; }
    @Override public int getHeight() { return 112; }
    @Override public IDrawable getIcon() { return icon; }
    private static Component tr(String key,Object... args) { return Component.translatable("jei.csgobox."+key,args); }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, AcquisitionRecipe recipe, IFocusGroup focuses) {
        builder.addOutputSlot(136,39).addItemStack(recipe.output()).setOutputSlotBackground();
        if (recipe.kind() == AcquisitionRecipe.Kind.CRAFTING) {
            for (int i=0;i<9;i++) builder.addInputSlot(8+i%3*18,22+i/3*18)
                    .addItemStack(recipe.inputs().get(i)).setStandardSlotBackground();
        } else if (recipe.kind() == AcquisitionRecipe.Kind.TRADE) {
            builder.addInputSlot(16,39).addItemStack(recipe.inputs().get(0)).setStandardSlotBackground();
            if (!recipe.inputs().get(1).isEmpty()) builder.addInputSlot(49,39)
                    .addItemStack(recipe.inputs().get(1)).setStandardSlotBackground();
        }
    }
    private void text(GuiGraphics graphics, Component line, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(1F, width/(float)Math.max(1,font.width(line)));
        graphics.pose().pushPose(); graphics.pose().translate(x,y,0); graphics.pose().scale(scale,scale,1);
        graphics.drawString(font,line,0,0,0xFF303030,false); graphics.pose().popPose();
    }
    @Override public void draw(AcquisitionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        text(graphics,tr(recipe.kind().name().toLowerCase(Locale.ROOT)),4,3,164);
        if (recipe.kind() == AcquisitionRecipe.Kind.CRAFTING || recipe.kind() == AcquisitionRecipe.Kind.TRADE)
            arrow.draw(graphics,91,39);
        if (recipe.kind() == AcquisitionRecipe.Kind.DROP) {
            var entity = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(recipe.source()));
            text(graphics,entity == null ? Component.literal(recipe.source()) : entity.getDescription(),4,29,117);
            text(graphics,tr("chance",String.format(Locale.ROOT,"%.3f%%",recipe.chance()*100)),4,48,117);
            text(graphics,Component.literal(recipe.source()),4,69,164);
        } else if (recipe.kind() == AcquisitionRecipe.Kind.TRADE) {
            var profession = new ResourceLocation(recipe.source());
            text(graphics,Component.translatable("entity."+profession.getNamespace()+".villager."+profession.getPath()),4,21,117);
            text(graphics,tr("trade_details",recipe.level(),recipe.uses(),recipe.xp()),4,69,164);
            text(graphics,tr("trade_candidate"),4,85,164);
        } else text(graphics,tr("shaped"),4,85,164);
        text(graphics,recipe.output().getHoverName(),4,101,164);
    }
}
