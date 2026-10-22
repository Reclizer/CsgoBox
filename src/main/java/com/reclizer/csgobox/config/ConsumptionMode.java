package com.reclizer.csgobox.config;

/** Items charged per successful opening. A key-only case remains usable. */
public enum ConsumptionMode {
    BOX("box", true, false), KEY("key", false, true), BOTH("both", true, true);
    private final String id;
    private final boolean box, key;
    ConsumptionMode(String id, boolean box, boolean key) { this.id=id; this.box=box; this.key=key; }
    public String id() { return id; }
    public boolean consumesBox() { return box; }
    public boolean requiresKey() { return key; }
    public boolean consumesKey() { return key; }
    public String translationKey() { return "editor.csgobox.consume_"+id; }
    public ConsumptionMode next() { return values()[(ordinal()+1)%values().length]; }
    public static boolean valid(String id) { return "box".equals(id) || "key".equals(id) || "both".equals(id); }
    public static ConsumptionMode parse(String id) {
        for (var mode : values()) if (mode.id.equals(id)) return mode;
        throw new IllegalArgumentException("editor.csgobox.bad_consumption");
    }
}
