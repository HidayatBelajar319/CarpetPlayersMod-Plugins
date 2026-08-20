package com.carpetplayers;

import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.rank.RankManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper/Spigot 1.21.11 plugin entry point - a port of the Fabric mod "Carpet Players".
 * Detected as a plugin via plugin.yml and JavaPlugin.
 */
public final class CarpetPlayersPlugin extends JavaPlugin {
    public static final String MOD_ID = "carpetplayers";
    public static CarpetPlayersPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        // Load/create the config file right at startup (server-side),
        // so the config file is available before players join.
        ModConfig.ensureLoaded();
        RankManager.init();
        AIProviderManager.instance().ensureLoaded();
        BotManager.registerCommands(this);
        BotManager.registerEvents(this);
        // Tick 20x/second on the server thread, equivalent to ServerTickEvents.END_SERVER_TICK.
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
