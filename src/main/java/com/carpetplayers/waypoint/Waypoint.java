package com.carpetplayers.waypoint;

/**
 * Data model for a single waypoint.
 * Stored as JSON in carpetplayers-waypoints.json in the config directory.
 */
public class Waypoint {
    private String name;
    private double x;
    private double y;
    private double z;
    private String dimension; // "overworld", "the_nether", "the_end"
    private int color;        // packed RGB: 0xRRGGBB
    private boolean enabled;
    private boolean isDeath;  // true for auto-created death waypoints

    /** Predefined colors */
    public static final int RED    = 0xFF5555;
    public static final int GREEN  = 0x55FF55;
    public static final int BLUE   = 0x5555FF;
    public static final int YELLOW = 0xFFFF55;
    public static final int PURPLE = 0xFF55FF;
    public static final int ORANGE = 0xFFAA00;
    public static final int WHITE  = 0xFFFFFF;
    public static final int CYAN   = 0x55FFFF;

    public Waypoint() {
        this.enabled = true;
        this.color = WHITE;
        this.dimension = "overworld";
    }

    public Waypoint(String name, double x, double y, double z, String dimension) {
        this();
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDeath() { return isDeath; }
    public void setDeath(boolean death) { isDeath = death; }

    /**
     * Resolve a color name to its packed int value.
     * Returns -1 if not recognized.
     */
    public static int resolveColor(String name) {
        switch (name.toLowerCase()) {
            case "red":     return RED;
            case "green":   return GREEN;
            case "blue":    return BLUE;
            case "yellow":  return YELLOW;
            case "purple":  return PURPLE;
            case "orange":  return ORANGE;
            case "white":   return WHITE;
            case "cyan":    return CYAN;
            default:        return -1;
        }
    }

    /**
     * Convert a packed RGB int to a color name, or "custom" if not predefined.
     */
    public static String colorName(int color) {
        if (color == RED)     return "red";
        if (color == GREEN)   return "green";
        if (color == BLUE)    return "blue";
        if (color == YELLOW)  return "yellow";
        if (color == PURPLE)  return "purple";
        if (color == ORANGE)  return "orange";
        if (color == WHITE)   return "white";
        if (color == CYAN)    return "cyan";
        return "custom";
    }

    /**
     * Format coordinates for display: "X=100 Y=64 Z=-200"
     */
    public String coordString() {
        return String.format("X=%.0f Y=%.0f Z=%.0f", x, y, z);
    }

    /**
     * Format as a colored prefix for chat/GUI display.
     */
    public String displayString() {
        return getName() + " [" + colorName(color) + "] " + coordString();
    }
}
