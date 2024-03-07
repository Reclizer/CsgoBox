package com.reclizer.csgobox.item;


import com.reclizer.csgobox.gui.CsgoBoxCraftMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.NetworkHooks;

import static com.reclizer.csgobox.utils.ItemNBT.isModLoaded;

public class ItemOpenBox extends Item {
    public ItemOpenBox() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);

        if(world.isClientSide){
            return ar;
        }


        //System.out.println(isModLoaded("crafttweaker"));

        execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);

        return ar;
    }





    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return;
        if (entity instanceof ServerPlayer _ent) {
            BlockPos pos = BlockPos.containing(x, y, z);
            NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {

                @Override
                public Component getDisplayName() {
                    return Component.literal("csgo_box_craft");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    return new CsgoBoxCraftMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));

                }





            }, pos);
        }
    }
}
