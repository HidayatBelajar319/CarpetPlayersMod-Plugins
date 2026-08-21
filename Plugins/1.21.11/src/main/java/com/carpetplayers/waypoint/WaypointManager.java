package com.carpetplayers.waypoint;

import com.carpetplayers.CarpetPlayersPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, List<Waypoint>> WAYPOINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> DEATH_HISTORY = new ConcurrentHashMap<>();
    private static File waypointsDir;

    public static void init() {
        waypointsDir = new File(CarpetPlayersPlugin.instance.getDataFolder(), "waypoints");
        waypointsDir.mkdirs();
    }

    public static void loadAll() {
        if (waypointsDir == null) init();
        File[] files = waypointsDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try {
                String uuid = file.getName().replace(".json", "");
                UUID playerUUID = UUID.fromString(uuid);
                Type listType = new TypeToken<List<Waypoint>>(){}.getType();
                try (Reader reader = new FileReader(file)) {
                    List<Waypoint> list = GSON.fromJson(reader, listType);
                    if (list != null) WAYPOINTS.put(playerUUID, list);
                }
            } catch (Exception e) {
                CarpetPlayersPlugin.logError("Failed to load waypoints: " + file.getName(), e);
            }
        }
    }

    public static void savePlayer(UUID uuid) {
        List<Waypoint> list = WAYPOINTS.get(uuid);
        if (list == null) list = Collections.emptyList();
        File file = new File(waypointsDir, uuid.toString() + ".json");
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(list, writer);
        } catch (Exception e) {
            CarpetPlayersPlugin.logError("Failed to save waypoints for " + uuid, e);
        }
    }

    public static void saveAll() {
        for (UUID uuid : WAYPOINTS.keySet()) savePlayer(uuid);
    }

    public static List<Waypoint> getWaypoints(UUID uuid) {
        return WAYPOINTS.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    public static boolean addWaypoint(UUID uuid, String name, Location loc, String color) {
        List<Waypoint> list = getWaypoints(uuid);
        for (Waypoint w : list) {
            if (w.name.equalsIgnoreCase(name)) return false;
        }
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        list.add(new Waypoint(name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldName, color, true, false));
        savePlayer(uuid);
        return true;
    }

    public static boolean removeWaypoint(UUID uuid, String name) {
        List<Waypoint> list = getWaypoints(uuid);
        boolean removed = list.removeIf(w -> w.name.equalsIgnoreCase(name));
        if (removed) savePlayer(uuid);
        return removed;
    }

    public static Waypoint findWaypoint(UUID uuid, String name) {
        for (Waypoint w : getWaypoints(uuid)) {
            if (w.name.equalsIgnoreCase(name)) return w;
        }
        return null;
    }

    public static boolean setColor(UUID uuid, String name, String color) {
        Waypoint w = findWaypoint(uuid, name);
        if (w == null) return false;
        w.color = color;
        savePlayer(uuid);
        return true;
    }

    public static boolean setEnabled(UUID uuid, String name, boolean enabled) {
        Waypoint w = findWaypoint(uuid, name);
        if (w == null) return false;
        w.enabled = enabled;
        savePlayer(uuid);
        return true;
    }

    /**
     * Death waypoint: Death -> Old Death -> removed on 3rd death.
     * Each death removes previous "Death", creates new "Death" waypoint.
     * On 3rd+ death, "Old Death" is also cleaned up.
     */
    public static void handleDeath(UUID uuid, Location loc) {
        List<Waypoint> list = getWaypoints(uuid);
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";

        // Remove "Death" if exists
        list.removeIf(w -> w.name.equalsIgnoreCase("Death"));
        // Remove "Old Death" if exists
        list.removeIf(w -> w.name.equalsIgnoreCase("Old Death"));

        // Add new "Death" waypoint
        list.add(new Waypoint("Death", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldName, "red", true, true));

        savePlayer(uuid);
    }

    public static void setWaypointsDir(File dir) {
        waypointsDir = dir;
    }
}
