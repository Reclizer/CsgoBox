package com.reclizer.csgobox.item;

import com.reclizer.csgobox.block.*;
import com.reclizer.csgobox.config.BoxAppearance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import javax.annotation.Nullable;

/** Decorative BlockItem, separate from anything that can select rewards. */
public final class EmptyBoxItem extends BlockItem {
    public static final String APPEARANCE_TAG="Appearance";
    public EmptyBoxItem() { super(ModBlocks.BOX.get(),new Properties().stacksTo(16)); }
    public static BoxAppearance appearance(ItemStack stack) {
        return stack.hasTag()?BoxAppearance.byId(stack.getTag().getString(APPEARANCE_TAG)):BoxAppearance.GOLD;
    }
    public static ItemStack create(BoxAppearance appearance) {
        var stack=new ItemStack(ModItems.ITEM_EMPTY_BOX.get());
        stack.getOrCreateTag().putString(APPEARANCE_TAG,appearance.id()); return stack;
    }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof PlacedBoxEntity box)) return false;
        box.setEmpty(); return true;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.csgobox.empty_box",Component.translatable(appearance(stack).translationKey()));
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltips.csgobox.empty_box").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
