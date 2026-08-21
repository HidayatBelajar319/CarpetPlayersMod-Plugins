package com.carpetplayers.waypoint;

import com.carpetplayers.CarpetPlayersMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all waypoints: CRUD, persistence, death waypoint logic.
 * Per-player waypoints stored in carpetplayers-waypoints/<uuid>.json
 */
public final class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>(){}.getType();
    private static File waypointsDir;

    /** In-memory waypoints per player UUID */
    private static final Map<UUID, List<Waypoint>> PLAYER_WAYPOINTS = new ConcurrentHashMap<>();

    /** Death count per player (for Death → Old Death → remove logic) */
    private static final Map<UUID, Integer> DEATH_COUNTS = new ConcurrentHashMap<>();

    private WaypointManager() {}

    public static void init() {
        waypointsDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "carpetplayers-waypoints");
        if (!waypointsDir.exists()) {
            waypointsDir.mkdirs();
        }
        CarpetPlayersMod.LOGGER.info("WaypointManager initialized. Dir: {}", waypointsDir.getAbsolutePath());
    }

    // ======================== CRUD ========================

    /**
     * Add a waypoint for a player. Returns true on success.
     */
    public static boolean addWaypoint(UUID playerUuid, Waypoint wp) {
        List<Waypoint> list = PLAYER_WAYPOINTS.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        // Duplicate name check
        for (Waypoint existing : list) {
            if (existing.getName().equalsIgnoreCase(wp.getName())) {
                return false;
            }
        }
        list.add(wp);
        savePlayer(playerUuid);
        return true;
    }

    /**
     * Remove a waypoint by name. Returns the removed waypoint or null.
     */
    public static Waypoint removeWaypoint(UUID playerUuid, String name) {
        List<Waypoint> list = PLAYER_WAYPOINTS.get(playerUuid);
        if (list == null) return null;
        Iterator<Waypoint> it = list.iterator();
        while (it.hasNext()) {
            Waypoint wp = it.next();
            if (wp.getName().equalsIgnoreCase(name)) {
                it.remove();
                savePlayer(playerUuid);
                return wp;
            }
        }
        return null;
    }

    /**
     * Find a waypoint by name (case-insensitive).
     */
    public static Waypoint findWaypoint(UUID playerUuid, String name) {
        List<Waypoint> list = PLAYER_WAYPOINTS.get(playerUuid);
        if (list == null) return null;
        for (Waypoint wp : list) {
            if (wp.getName().equalsIgnoreCase(name)) {
                return wp;
            }
        }
        return null;
    }

    /**
     * Get all waypoints for a player.
     */
    public static List<Waypoint> getWaypoints(UUID playerUuid) {
        return PLAYER_WAYPOINTS.getOrDefault(playerUuid, Collections.emptyList());
    }

    /**
     * Get only enabled waypoints for a player.
     */
    public static List<Waypoint> getEnabledWaypoints(UUID playerUuid) {
        return getWaypoints(playerUuid).stream()
                .filter(Waypoint::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Change color of a waypoint.
     */
    public static boolean setColor(UUID playerUuid, String name, int color) {
        Waypoint wp = findWaypoint(playerUuid, name);
        if (wp == null) return false;
        wp.setColor(color);
        savePlayer(playerUuid);
        return true;
    }

    /**
     * Enable/disable a waypoint.
     */
    public static boolean setEnabled(UUID playerUuid, String name, boolean enabled) {
        Waypoint wp = findWaypoint(playerUuid, name);
        if (wp == null) return false;
        wp.setEnabled(enabled);
        savePlayer(playerUuid);
        return true;
    }

    // ======================== DEATH WAYPOINT ========================

    /**
     * Handle player death: creates/renames/removes death waypoints.
     * Logic:
     *   death_count == 0 → create "Death" at position
     *   death_count == 1 → rename previous "Death" to "Old Death", create new "Death"
     *   death_count >= 2 → remove "Old Death", create new "Death", reset count
     *
     * @param dimKey The ResourceKey for the dimension (e.g. Level.OVERWORLD)
     */
    public static Waypoint handleDeath(UUID playerUuid, double x, double y, double z, ResourceKey<Level> dimKey) {
        int count = DEATH_COUNTS.getOrDefault(playerUuid, 0);
        String dimension = dimensionToString(dimKey);

        // Remove previous death waypoints based on count
        if (count >= 1) {
            // Remove any existing "Death" waypoint
            removeWaypoint(playerUuid, "Death");
        }
        if (count >= 2) {
            // Remove "Old Death" waypoint
            removeWaypoint(playerUuid, "Old Death");
            // Reset counter — next death starts fresh
            DEATH_COUNTS.put(playerUuid, 0);
            count = 0;
        }

        // On second death, rename the current "Death" to "Old Death" before removing it
        if (count == 1) {
            Waypoint oldDeath = findWaypoint(playerUuid, "Death");
            if (oldDeath != null) {
                oldDeath.setName("Old Death");
                // save happens in addWaypoint below
            }
        }

        // Create new "Death" waypoint
        Waypoint deathWp = new Waypoint("Death", x, y, z, dimension);
        deathWp.setColor(Waypoint.RED);
        deathWp.setDeath(true);

        if (count == 1) {
            // "Old Death" already renamed above, just save the player file
            savePlayer(playerUuid);
        }

        boolean added = addWaypoint(playerUuid, deathWp);

        // Increment death counter
        DEATH_COUNTS.put(playerUuid, count + 1);

        CarpetPlayersMod.LOGGER.info("Death waypoint for {}: count={}, pos=({},{},{})", playerUuid, count + 1, x, y, z);
        return deathWp;
    }

    // ======================== PERSISTENCE ========================

    private static File getPlayerFile(UUID playerUuid) {
        return new File(waypointsDir, playerUuid.toString() + ".json");
    }

    public static void savePlayer(UUID playerUuid) {
        List<Waypoint> list = PLAYER_WAYPOINTS.get(playerUuid);
        if (list == null) return;
        try {
            File file = getPlayerFile(playerUuid);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(list, writer);
            }
        } catch (Exception e) {
            CarpetPlayersMod.LOGGER.error("Failed to save waypoints for {}", playerUuid, e);
        }
    }

    public static void loadPlayer(UUID playerUuid) {
        File file = getPlayerFile(playerUuid);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            List<Waypoint> list = GSON.fromJson(reader, LIST_TYPE);
            if (list != null) {
                PLAYER_WAYPOINTS.put(playerUuid, list);
            }
        } catch (Exception e) {
            CarpetPlayersMod.LOGGER.error("Failed to load waypoints for {}", playerUuid, e);
        }
    }

    /**
     * Load all player waypoint files from disk.
     */
    public static void loadAll() {
        if (waypointsDir == null || !waypointsDir.exists()) return;
        File[] files = waypointsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            String uuidStr = file.getName().replace(".json", "");
            try {
                UUID uuid = UUID.fromString(uuidStr);
                loadPlayer(uuid);
            } catch (IllegalArgumentException ignored) {}
        }
        CarpetPlayersMod.LOGGER.info("Loaded waypoints for {} players", PLAYER_WAYPOINTS.size());
    }

    /**
     * Save all players to disk.
     */
    public static void saveAll() {
        for (UUID uuid : PLAYER_WAYPOINTS.keySet()) {
            savePlayer(uuid);
        }
    }

    // ======================== HELPERS ========================

    public static String dimensionToString(ResourceKey<Level> dimKey) {
        if (dimKey == Level.OVERWORLD) return "overworld";
        if (dimKey == Level.NETHER) return "the_nether";
        if (dimKey == Level.END) return "the_end";
        return dimKey.location().toString();
    }

    public static ResourceKey<Level> stringToDimension(String s) {
        if ("overworld".equals(s)) return Level.OVERWORLD;
        if ("the_nether".equals(s)) return Level.NETHER;
        if ("the_end".equals(s)) return Level.END;
        return Level.OVERWORLD;
    }

    /**
     * Teleport a player to a waypoint.
     */
    public static boolean teleportTo(ServerPlayer player, Waypoint wp) {
        if (wp == null) return false;
        player.teleportTo(wp.getX(), wp.getY(), wp.getZ());
        return true;
    }
}
