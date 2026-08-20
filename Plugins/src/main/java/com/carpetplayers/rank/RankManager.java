package com.carpetplayers.rank;

import com.carpetplayers.CarpetPlayersPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public final class RankManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Rank> ranks = new HashMap<>();
    private static Rank defaultRank = Rank.USER;
    private static File configFile;

    public static void init() {
        configFile = new File(CarpetPlayersPlugin.instance.getDataFolder(), "carpetplayers-ranks.json");
        load();
    }

    public static Rank getRank(UUID playerUuid) {
        return ranks.getOrDefault(playerUuid, defaultRank);
    }

    public static void setRank(UUID playerUuid, Rank rank) {
        ranks.put(playerUuid, rank);
        save();
    }

    public static void removeRank(UUID playerUuid) {
        ranks.remove(playerUuid);
        save();
    }

    public static Map<UUID, Rank> getAllRanks() {
        return Collections.unmodifiableMap(ranks);
    }

    public static Rank getDefaultRank() { return defaultRank; }

    public static void setDefaultRank(Rank rank) {
        defaultRank = rank;
        save();
    }

    public static int getMaxBots(UUID playerUuid) {
        return getRank(playerUuid).getMaxBots();
    }

    public static boolean hasPermission(UUID playerUuid, int requiredLevel) {
        return getRank(playerUuid).getPermissionLevel() >= requiredLevel;
    }

    private static void load() {
        if (!configFile.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> raw = GSON.fromJson(reader, type);
            if (raw != null) {
                for (Map.Entry<String, String> entry : raw.entrySet()) {
                    try {
                        ranks.put(UUID.fromString(entry.getKey()), Rank.fromName(entry.getValue()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            CarpetPlayersPlugin.logError("Failed to load ranks", e);
        }
    }

    public static void save() {
        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, Rank> entry : ranks.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue().getName());
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            try (Writer writer = new FileWriter(configFile)) {
                GSON.toJson(raw, writer);
            }
        } catch (Exception e) {
            CarpetPlayersPlugin.logError("Failed to save ranks", e);
        }
    }

    private RankManager() {}
}
