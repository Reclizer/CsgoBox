package com.reclizer.csgobox.item;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.config.CsgoBoxManage;
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
import net.minecraftforge.fml.util.thread.EffectiveSide;
import javax.annotation.Nullable;
import java.util.List;

public class ItemCsgoBox extends Item {
    public static final String POOL_ID_TAG = "PoolId";

    public ItemCsgoBox() { super(new Properties().stacksTo(16).rarity(Rarity.EPIC)); }

    public static String boxId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (!(stack.getItem() instanceof ItemCsgoBox) || tag == null || !tag.contains(POOL_ID_TAG, Tag.TAG_STRING))
            return "";
        String id = tag.getString(POOL_ID_TAG);
        return BoxDefinition.validId(id) ? id : "";
    }

    /** Identity only; names and rewards are resolved from the current server configuration. */
    public static ItemStack setBoxInfo(BoxDefinition info, ItemStack stack) {
        return setBoxId(info.id(), stack);
    }

    public static ItemStack setBoxId(String id, ItemStack stack) {
        if (!BoxDefinition.validId(id)) throw new IllegalArgumentException("Invalid pool id: " + id);
        stack.getOrCreateTag().remove("BoxItemInfo");
        stack.getOrCreateTag().putString(POOL_ID_TAG, id);
        return stack;
    }

    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = super.getShareTag(stack);
        if (tag == null) return null;
        CompoundTag safe = tag.copy();
        // Obsolete embedded definitions are neither used nor sent to clients.
        safe.remove("BoxItemInfo");
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
        String id = boxId(stack);
        if (id.isEmpty()) return super.getName(stack);
        if (EffectiveSide.get().isServer()) {
            BoxDefinition box = CsgoBoxManage.find(id);
            return Component.literal(box == null ? id : box.name());
        }
        return Component.literal(BoxCatalog.displayName(id));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltips.csgobox.item.cs_box").withStyle(ChatFormatting.GRAY));
        if (!boxId(stack).isEmpty()) tooltip.add(Component.literal(boxId(stack)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
