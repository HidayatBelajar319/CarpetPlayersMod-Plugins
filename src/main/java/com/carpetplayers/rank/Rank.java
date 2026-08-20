package com.carpetplayers.rank;

public enum Rank {
    ADMIN("admin", 4, -1),      // -1 = unlimited bots
    MODERATOR("moderator", 2, 10),
    USER("user", 0, 0);

    private final String name;
    private final int permissionLevel; // maps to Minecraft op-like levels
    private final int maxBots;

    Rank(String name, int permissionLevel, int maxBots) {
        this.name = name;
        this.permissionLevel = permissionLevel;
        this.maxBots = maxBots;
    }

    public String getName() {
        return name;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public int getMaxBots() {
        return maxBots;
    }

    public static Rank fromName(String name) {
        for (Rank rank : values()) {
            if (rank.name.equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return USER;
    }
}
