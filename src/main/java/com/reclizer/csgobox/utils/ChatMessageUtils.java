package com.reclizer.csgobox.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for sending styled chat messages related to box rewards
 */
public class ChatMessageUtils {
    
    /**
     * Sends a beautiful message to the player about an item they just received
     * 
     * @param player The player to send the message to
     * @param itemStack The item the player received
     * @param quantity The quantity of the item received
     * @param grade The grade/rarity of the item (1-5)
     */
    public static void sendItemObtainedMessage(Player player, ItemStack itemStack, int quantity, int grade) {
        if (player == null || itemStack.isEmpty()) return;
        
        // Create decorative separator line
        MutableComponent separator = Component.literal("★ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ★")
                .withStyle(getColorForGrade(grade))
                .withStyle(ChatFormatting.BOLD);
        
        // Get item name with appropriate color
        Component itemName = itemStack.getHoverName().copy()
                .withStyle(getColorForGrade(grade))
                .withStyle(ChatFormatting.BOLD);
        
        // Title message
        MutableComponent title = Component.literal("» UNBOXED! «")
                .withStyle(getColorForGrade(grade))
                .withStyle(ChatFormatting.BOLD);
        
        // Create the message with quantity if needed
        MutableComponent message;
        if (quantity > 1) {
            message = Component.literal("You obtained ")
                    .append(Component.literal("" + quantity).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD))
                    .append(Component.literal(" × "))
                    .append(itemName);
        } else {
            message = Component.literal("You obtained ")
                    .append(itemName);
        }
        
        // Send the full formatted message
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(separator);
        player.sendSystemMessage(title);
        player.sendSystemMessage(message);
        
        // Add grade text
        MutableComponent gradeText = Component.translatable("gui.csgobox.csgo_box.grade" + grade)
                .withStyle(getColorForGrade(grade));
        player.sendSystemMessage(gradeText);
        
        player.sendSystemMessage(separator);
        player.sendSystemMessage(Component.literal(""));
    }
    
    /**
     * Gets the appropriate ChatFormatting color based on item grade/rarity
     * 
     * @param grade The grade/rarity level (1-5)
     * @return ChatFormatting color
     */
    private static ChatFormatting getColorForGrade(int grade) {
        return switch (grade) {
            case 1 -> ChatFormatting.BLUE;       // Military grade
            case 2 -> ChatFormatting.DARK_BLUE;  // Restricted
            case 3 -> ChatFormatting.DARK_PURPLE;// Classified
            case 4 -> ChatFormatting.RED;        // Covert
            case 5 -> ChatFormatting.GOLD;       // Legendary
            default -> ChatFormatting.WHITE;
        };
    }
}