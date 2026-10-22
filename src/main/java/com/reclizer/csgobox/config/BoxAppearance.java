package com.reclizer.csgobox.config;

import net.minecraft.util.StringRepresentable;
import java.util.Arrays;

/** Stable configuration IDs; model numbers are private to the item resource overrides. */
public enum BoxAppearance implements StringRepresentable {
    GOLD("gold", 1, .5, 13.75, 8.25),
    ORANGE("orange", 2, 2.5, 13.5, 8.5),
    GREEN("green", 3, 1.75, 14, 3.5),
    ROSE_BLUE("rose_blue", 4, 1.75, 14.5, 10.25),
    YELLOW_METAL("yellow_metal", 5, 1.75, 14.5, 10.25);

    private final String id;
    private final int modelNumber;
    private final double front, back, height;
    BoxAppearance(String id, int modelNumber, double front, double back, double height) {
        this.id = id; this.modelNumber = modelNumber;
        this.front = front; this.back = back; this.height = height;
    }
    @Override public String getSerializedName() { return id; }
    public String id() { return id; }
    public String translationKey() { return "appearance.csgobox." + id; }
    public int modelNumber() { return modelNumber; }
    public double front() { return front; }
    public double back() { return back; }
    public double height() { return height; }
    public double hingeY() { return switch(this) { case GOLD -> 6.6/16; case ORANGE -> 6.32/16; case GREEN -> 1.64/16; default -> 8.31/16; }; }
    public double hingeZ() { return switch(this) { case GOLD -> 13.4/16; case ORANGE -> 13.12/16; case GREEN -> 13.62/16; default -> 14.12/16; }; }
    public static boolean valid(String id) { return Arrays.stream(values()).anyMatch(value -> value.id.equals(id)); }
    public static BoxAppearance byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(GOLD);
    }
}
