package com.reclizer.csgobox.item;

import java.util.List;

/** Client display cache populated only by the connected server. No configuration reads. */
public final class BoxCatalog {
    private static List<String> names = List.of();
    private BoxCatalog() {}
    public static List<String> names() { return names; }
    public static void replace(List<String> next) { names = List.copyOf(next); }
}
