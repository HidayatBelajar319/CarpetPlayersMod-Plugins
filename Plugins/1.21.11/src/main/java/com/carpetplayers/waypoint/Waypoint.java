package com.carpetplayers.waypoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Waypoint {
    public String name;
    public int x;
    public int y;
    public int z;
    public String world;
    public String color;
    public boolean enabled;
    public boolean isDeath;

    public Waypoint() {}

    public Waypoint(String name, int x, int y, int z, String world, String color, boolean enabled, boolean isDeath) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.color = color;
        this.enabled = enabled;
        this.isDeath = isDeath;
    }

    public String coordString() {
        return x + ", " + y + ", " + z;
    }

    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("white", "\u00a7f");
        COLOR_MAP.put("gold", "\u00a76");
        COLOR_MAP.put("yellow", "\u00a7e");
        COLOR_MAP.put("aqua", "\u00a7b");
        COLOR_MAP.put("red", "\u00a7c");
        COLOR_MAP.put("light_purple", "\u00a7d");
        COLOR_MAP.put("blue", "\u00a79");
        COLOR_MAP.put("green", "\u00a7a");
        COLOR_MAP.put("gray", "\u00a77");
        COLOR_MAP.put("dark_gray", "\u00a78");
        COLOR_MAP.put("dark_aqua", "\u00a73");
        COLOR_MAP.put("dark_red", "\u00a74");
        COLOR_MAP.put("dark_purple", "\u00a75");
        COLOR_MAP.put("dark_blue", "\u00a71");
        COLOR_MAP.put("dark_green", "\u00a72");
        COLOR_MAP.put("black", "\u00a70");
    }

    public static String resolveColor(String name) {
        if (name == null) return "\u00a7f";
        String code = COLOR_MAP.get(name.toLowerCase());
        return code != null ? code : "\u00a7f";
    }

    public static String colorName(String code) {
        for (Map.Entry<String, String> e : COLOR_MAP.entrySet()) {
            if (e.getValue().equals(code)) return e.getKey();
        }
        return "white";
    }

    public static boolean isValidColor(String name) {
        return COLOR_MAP.containsKey(name.toLowerCase());
    }

    public static Set<String> validColors() {
        return COLOR_MAP.keySet();
    }
}
