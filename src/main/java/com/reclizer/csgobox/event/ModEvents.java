package com.reclizer.csgobox.event;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.server.BoxService;
import com.reclizer.csgobox.server.ConfiguredAcquisition;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.IOException;

@Mod.EventBusSubscriber(modid = CsgoBox.MODID)
public final class ModEvents {
    @SubscribeEvent
    public static void start(ServerStartingEvent event) {
        BoxService.clearSessions();
        try {
            CsgoBox.LOGGER.info("Loaded {} box definitions", CsgoBoxManage.loadConfigBox());
            ConfiguredAcquisition.refresh(event.getServer());
        } catch (IOException ex) {
            // Refuse to start with an invalid economy configuration.
            throw new IllegalStateException("Cannot load config/csbox/box.json", ex);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { BoxService.clearSessions(); ConfiguredAcquisition.clear(); }

    @SubscribeEvent
    public static void datapacks(net.minecraftforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) ConfiguredAcquisition.refresh(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        BoxService.logout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BoxService.syncCatalog(player);
            ConfiguredAcquisition.unlockRecipes(player);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        ConfiguredAcquisition.drops(event);
    }

    @SubscribeEvent
    public static void protectOpening(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof com.reclizer.csgobox.block.PlacedBoxEntity box && box.isOpening()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("message.csgobox.ground_busy"),true);
        }
    }
    @SubscribeEvent
    public static void protectOpeningFromExplosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> event.getLevel().getBlockEntity(pos)
                instanceof com.reclizer.csgobox.block.PlacedBoxEntity box && box.isOpening());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csgobox").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    try {
                        int count = CsgoBoxManage.loadConfigBox();
                        ConfiguredAcquisition.refresh(context.getSource().getServer());
                        BoxService.clearSessions();
                        context.getSource().getServer().getPlayerList().getPlayers().forEach(BoxService::syncCatalog);
                        context.getSource().sendSuccess(() -> Component.translatable("message.csgobox.reloaded", count), true);
                        return count;
                    } catch (IOException ex) {
                        CsgoBox.LOGGER.warn("Box reload rejected; keeping previous configuration", ex);
                        context.getSource().sendFailure(Component.translatable("message.csgobox.config_error", ex.getMessage()));
                        return 0;
                    }
                }))
                .then(Commands.literal("give").then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                CsgoBoxManage.boxes().stream().map(box -> box.id()), builder))
                        .executes(context -> {
                            var box = CsgoBoxManage.find(ResourceLocationArgument.getId(context, "id").toString());
                            if (box == null) {
                                context.getSource().sendFailure(Component.translatable("message.csgobox.unknown_box"));
                                return 0;
                            }
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            BoxService.deliver(player, ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get())));
                            return 1;
                        }))));
    }
}
