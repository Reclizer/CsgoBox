package com.reclizer.csgobox.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.server.BoxService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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
        } catch (IOException ex) {
            // Refuse to start with an invalid economy configuration.
            throw new IllegalStateException("Cannot load config/csbox/box.json", ex);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { BoxService.clearSessions(); }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        BoxService.logout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BoxService.syncCatalog(player);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        for (var box : CsgoBoxManage.boxes()) {
            if (box.drop() > 0 && box.entity().contains(id) && entity.getRandom().nextFloat() < box.drop()) {
                var drop = new net.minecraft.world.entity.item.ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                        ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get())));
                event.getDrops().add(drop);
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csgobox").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    try {
                        int count = CsgoBoxManage.loadConfigBox();
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
                .then(Commands.literal("give").then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                CsgoBoxManage.boxes().stream().map(box -> box.name()), builder))
                        .executes(context -> {
                            var box = CsgoBoxManage.find(StringArgumentType.getString(context, "name"));
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
