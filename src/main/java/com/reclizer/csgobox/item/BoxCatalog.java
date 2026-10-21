package com.reclizer.csgobox.item;

import com.reclizer.csgobox.config.BoxDefinition;
import java.util.*;

/** Client display cache populated only by the connected server. No configuration reads. */
public final class BoxCatalog {
    public record Entry(String id, String name) {}
    private static Map<String, Entry> catalog = Map.of();
    private BoxCatalog() {}
    public static Collection<Entry> entries() { return catalog.values(); }
    public static String displayName(String id) {
        Entry entry = catalog.get(id);
        return entry == null ? id : entry.name();
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
    }
}
