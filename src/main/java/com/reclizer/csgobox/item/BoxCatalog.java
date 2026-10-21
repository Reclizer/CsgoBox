package com.reclizer.csgobox.item;

import com.reclizer.csgobox.config.BoxDefinition;
import com.reclizer.csgobox.packet.RewardName;
import com.reclizer.csgobox.utils.ColorTools;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Client display cache populated only by the connected server. No configuration reads. */
public final class BoxCatalog {
    public record Entry(String id, String name) {}
    private static Map<String, Entry> catalog = Map.of();
    private static final Map<String, List<RewardName>> rewardNames = new HashMap<>();
    private static final Map<String, List<Component>> tooltipLines = new HashMap<>();
    private static Language tooltipLanguage;
    private BoxCatalog() {}
    public static Collection<Entry> entries() { return catalog.values(); }
    public static String displayName(String id) {
        Entry entry = catalog.get(id);
        return entry == null ? id : entry.name();
    }

    public static void replaceRewardNames(String id, List<RewardName> rewards) {
        if (rewards.size() > 256) throw new IllegalArgumentException("Oversized reward-name summary");
        if (!catalog.containsKey(id)) return;
        rewardNames.put(id, List.copyOf(rewards));
        tooltipLines.remove(id);
    }

    /** Resolve translated names only when the summary or client language changes. */
    public static void appendRewardTooltip(String id, boolean expanded, List<Component> tooltip) {
        List<RewardName> rewards = rewardNames.get(id);
        if (rewards == null) {
            tooltip.add(Component.translatable("tooltips.csgobox.rewards_unavailable").withStyle(ChatFormatting.GRAY));
        } else if (rewards.isEmpty()) {
            tooltip.add(Component.translatable("tooltips.csgobox.rewards_empty").withStyle(ChatFormatting.GRAY));
        } else if (!expanded) {
            tooltip.add(Component.translatable("tooltips.csgobox.reward_count", rewards.size()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltips.csgobox.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            if (tooltipLanguage != Language.getInstance()) {
                tooltipLanguage = Language.getInstance();
                tooltipLines.clear();
            }
            tooltip.addAll(tooltipLines.computeIfAbsent(id, ignored -> rewards.stream().map(reward -> {
                // Flatten after client translation: custom nested styles cannot override the grade colour.
                String name = ChatFormatting.stripFormatting(reward.name().getString())
                        .replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
                return (Component) Component.literal(name).withStyle(style -> style.withColor(ColorTools.colorItems(reward.grade()) & 0xFFFFFF));
            }).toList()));
        }
    }
    public static void replace(List<Entry> next) {
        if (next.size() > 512) throw new IllegalArgumentException("Oversized pool catalog");
        Map<String, Entry> replacement = new LinkedHashMap<>();
        for (Entry entry : next) {
            if (!BoxDefinition.validId(entry.id()) || entry.name() == null || entry.name().isBlank()
                    || entry.name().length() > 128 || replacement.putIfAbsent(entry.id(), entry) != null)
                throw new IllegalArgumentException("Invalid pool catalog");
        }
        catalog = Collections.unmodifiableMap(replacement);
        // Catalog and per-pool summaries are ordered on the same connection. Never show stale loot names.
        rewardNames.clear();
        tooltipLines.clear();
        tooltipLanguage = null;
    }
}
