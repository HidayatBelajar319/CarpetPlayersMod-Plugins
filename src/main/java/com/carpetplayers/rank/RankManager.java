package com.carpetplayers.rank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class RankManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Rank> ranks = new HashMap<>();
    private static Rank defaultRank = Rank.USER;
    private static Path configPath;

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("carpetplayers-ranks.json");
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

    public static Rank getDefaultRank() {
        return defaultRank;
    }

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
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> raw = GSON.fromJson(reader, type);
            if (raw != null) {
                for (Map.Entry<String, String> entry : raw.entrySet()) {
                    try {
                        ranks.put(UUID.fromString(entry.getKey()), Rank.fromName(entry.getValue()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CarpetPlayers] Failed to load ranks: " + e.getMessage());
        }
    }

    public static void save() {
        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, Rank> entry : ranks.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue().getName());
        }
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(raw, writer);
        } catch (Exception e) {
            System.err.println("[CarpetPlayers] Failed to save ranks: " + e.getMessage());
        }
    }

    private RankManager() {
    }
}
