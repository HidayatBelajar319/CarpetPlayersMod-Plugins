package com.carpetplayers.config;

import com.carpetplayers.CarpetPlayersMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    public boolean useItemEnabled = true;
    public boolean interactiveEnabled = true;
    public boolean multiWeaponEnabled = true;
    public boolean tapWEnabled = false;
    public boolean tapAEnabled = false;
    public boolean tapSEnabled = false;
    public boolean tapDEnabled = false;
    public int maxBots = 50;
    public int wanderRadius = 16;
    public int pvpTargetRadius = 16;
    public int baseTargetRadius = 8;
    public boolean debugLogging = true;
    public boolean rankSystemEnabled = false;  // opt-in rank system
    public int moderatorMaxBots = 10;           // override for moderator max bots
    public int userMaxBots = 0;                 // override for user max bots (0 = cannot spawn)
    public boolean persistentBots = false;     // save bot configs across restarts
    public int autoSaveIntervalMinutes = 5;    // auto-save interval in minutes
    public int maxCreditsPerAction = 5;    // max credits (thousands of tokens) per AI action
    public boolean creditTrackingEnabled = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static ModConfig instance = new ModConfig();
    private static File configFile;

    private ModConfig() {}

    public static void ensureLoaded() {
        if (configFile == null) {
            configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(),
                    "carpetplayers-config.json");
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) {
                        instance = loaded;
                    }
                } catch (Exception e) {
                    CarpetPlayersMod.LOGGER.error("Failed to load Carpet Players config", e);
                }
            } else {
                save();
            }
        }
    }

    public static void save() {
        if (configFile == null) {
            return;
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            CarpetPlayersMod.LOGGER.error("Failed to save Carpet Players config", e);
        }
    }

    public Map<String, Boolean> tapControls() {
        Map<String, Boolean> controls = new HashMap<>();
        controls.put("w-tap", tapWEnabled);
        controls.put("a-tap", tapAEnabled);
        controls.put("s-tap", tapSEnabled);
        controls.put("d-tap", tapDEnabled);
        return controls;
    }

    public void setTap(String tap, boolean enabled) {
        switch (tap) {
            case "w-tap":
                tapWEnabled = enabled;
                break;
            case "a-tap":
                tapAEnabled = enabled;
                break;
            case "s-tap":
                tapSEnabled = enabled;
                break;
            case "d-tap":
                tapDEnabled = enabled;
                break;
            default:
                break;
        }
    }
}
