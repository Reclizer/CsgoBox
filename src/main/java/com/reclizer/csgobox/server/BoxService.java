package com.reclizer.csgobox.server;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.config.CsgoBoxManage;
import com.reclizer.csgobox.item.ItemCsgoBox;
import com.reclizer.csgobox.item.ModItems;
import com.reclizer.csgobox.packet.*;
import com.reclizer.csgobox.utils.ItemNBT;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class BoxService {
    private static final OpeningSessions<ItemStack> SESSIONS = new OpeningSessions<>();
    private static final Random RANDOM = new Random();
    private BoxService() {}

    public static void preview(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        ItemStack held = player.getMainHandItem();
        BoxDefinition box = CsgoBoxManage.find(ItemCsgoBox.boxId(held));
        if (box == null) {
            player.sendSystemMessage(Component.translatable("message.csgobox.unknown_box"));
            return;
        }
        var session = SESSIONS.begin(player.getUUID(), player.getInventory().selected, held.copy(), player.server.getTickCount());
        List<DisplayReward> display = new ArrayList<>();
        for (int grade = 1; grade <= 4; grade++) {
            for (String data : box.items(grade)) display.add(new DisplayReward(ItemNBT.parseStrict(data), grade));
        }
        // Gold prizes, weights, and entity-drop rules never go into preview packets.
        if (!box.grade5().isEmpty()) display.add(new DisplayReward(new ItemStack(ModItems.ITEM_CSGO_KEY0.get()), 5));
        Networking.sendToPlayerClient(new PacketBoxPreview(session.token(),
                ItemCsgoBox.setBoxInfo(box, new ItemStack(ModItems.ITEM_CSGOBOX.get())),
                ItemNBT.parseStrict(box.key()), display), player);
    }

    public static void open(ServerPlayer player, UUID token) {
        var session = SESSIONS.consume(player.getUUID(), token, player.server.getTickCount());
        ItemStack held = player.getMainHandItem();
        if (session == null || !player.isAlive() || player.isSpectator()
                || player.getInventory().selected != session.slot() || held.isEmpty()
                || !ItemStack.isSameItemSameTags(held, session.snapshot())) {
            fail(player, token, "message.csgobox.expired");
            return;
        }
        // Resolve the current pool by stable ID. Item NBT never selects the rewards or key.
        BoxDefinition box = CsgoBoxManage.find(ItemCsgoBox.boxId(held));
        if (box == null) { fail(player, token, "message.csgobox.unknown_box"); return; }
        var result = BoxOpening.open(player, box, RANDOM);
        if (!result.success()) { fail(player, token, result.error()); return; }
        // Award before animation: closing a screen or disconnecting cannot cancel or repeat it.
        Networking.sendToPlayerClient(new PacketBoxResult(token, result.reward(), result.grade()), player);
    }

    public static void deliver(ServerPlayer player, ItemStack stack) {
        // Inventory.add discards overflow for creative players. Preserve rewards in every game mode.
        var inventory = player.getInventory();
        for (ItemStack existing : inventory.items) {
            if (stack.isEmpty()) break;
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                int amount = Math.min(stack.getCount(), Math.max(0, Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize()) - existing.getCount()));
                existing.grow(amount);
                stack.shrink(amount);
            }
        }
        for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
            if (inventory.getItem(slot).isEmpty())
                inventory.setItem(slot, stack.split(Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize())));
        }
        inventory.setChanged();
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void fail(ServerPlayer player, UUID token, String message) {
        player.sendSystemMessage(Component.translatable(message));
        Networking.sendToPlayerClient(new PacketBoxResult(token, ItemStack.EMPTY, 0), player);
    }

    public static void logout(UUID player) { SESSIONS.remove(player); }
    public static void clearSessions() { SESSIONS.clear(); }
    public static void syncCatalog(ServerPlayer player) {
        Networking.sendToPlayerClient(new PacketBoxCatalog(CsgoBoxManage.boxes().stream().map(box -> new com.reclizer.csgobox.item.BoxCatalog.Entry(box.id(), box.name())).toList()), player);
    }
}
