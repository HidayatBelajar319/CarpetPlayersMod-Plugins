package com.carpetplayers;

import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.config.ModConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point plugin Paper/Spigot 1.16.5 — port dari mod Fabric "Carpet Players".
 * Terdeteksi sebagai plugin via plugin.yml dan JavaPlugin.
 */
public final class CarpetPlayersPlugin extends JavaPlugin {
    public static final String MOD_ID = "carpetplayers";
    public static CarpetPlayersPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        // Load/buat file konfigurasi langsung saat startup (server-side),
        // sehingga file config tersedia sebelum player join.
        ModConfig.ensureLoaded();
        AIProviderManager.instance().ensureLoaded();
        BotManager.registerCommands(this);
        BotManager.registerEvents(this);
        // Tick 20x/detik di server thread, setara ServerTickEvents.END_SERVER_TICK.
        getServer().getScheduler().runTaskTimer(this, BotManager::tick, 0L, 1L);
        getLogger().info("Carpet Players plugin loaded!");
    }

    @Override
    public void onDisable() {
        BotManager.removeAllBots();
        AIProviderManager.instance().shutdown();
        getLogger().info("Carpet Players plugin disabled.");
    }

    public static void log(String message) {
        Logger logger = instance != null ? instance.getLogger() : Logger.getLogger(MOD_ID);
        logger.info(message);
    }

    public static void logError(String message, Throwable t) {
        Logger logger = instance != null ? instance.getLogger() : Logger.getLogger(MOD_ID);
        logger.log(Level.SEVERE, message, t);
    }
}
