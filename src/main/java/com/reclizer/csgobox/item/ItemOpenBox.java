package com.reclizer.csgobox.item;

import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class ItemOpenBox extends Item {
    public ItemOpenBox() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.hasPermissions(2)) return InteractionResultHolder.fail(held);
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new CsgoBoxCraftMenu(id, inventory, null),
                    Component.translatable("gui.csgobox.csgo_box_craft.box_name")));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
