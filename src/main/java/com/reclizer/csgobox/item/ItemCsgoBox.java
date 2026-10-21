package com.reclizer.csgobox.item;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.server.BoxService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public class ItemCsgoBox extends Item {
    public static final String BOX_INFO_TAG = "BoxItemInfo";

    public ItemCsgoBox() { super(new Properties().stacksTo(16).rarity(Rarity.EPIC)); }

    public static String boxName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (!(stack.getItem() instanceof ItemCsgoBox) || tag == null || !tag.contains(BOX_INFO_TAG, Tag.TAG_COMPOUND))
            return "";
        return tag.getCompound(BOX_INFO_TAG).getString("name");
    }

    /** Store identity only. Legacy full-NBT boxes still resolve by their existing name. */
    public static ItemStack setBoxInfo(BoxDefinition info, ItemStack stack) {
        return setBoxName(info.name(), stack);
    }

    public static ItemStack setBoxName(String name, ItemStack stack) {
        CompoundTag identity = new CompoundTag();
        identity.putString("name", name);
        stack.getOrCreateTag().put(BOX_INFO_TAG, identity);
        return stack;
    }

    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = super.getShareTag(stack);
        if (tag == null) return null;
        CompoundTag safe = tag.copy();
        if (safe.contains(BOX_INFO_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag identity = new CompoundTag();
            identity.putString("name", boxName(stack));
            safe.put(BOX_INFO_TAG, identity);
        }
        return safe;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer serverPlayer) BoxService.preview(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String name = boxName(stack);
        return name.isBlank() ? super.getName(stack) : Component.literal(name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltips.csgobox.item.cs_box").withStyle(ChatFormatting.GRAY));
    }
}
