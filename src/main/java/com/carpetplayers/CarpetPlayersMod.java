package com.carpetplayers;

import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarpetPlayersMod implements ModInitializer {
    public static final String MOD_ID = "carpetplayers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Load/buat file konfigurasi langsung saat startup (server-side),
        // sehingga file config tersedia sebelum player join.
        ModConfig.ensureLoaded();
        AIProviderManager.instance().ensureLoaded();
        CommandRegistrationCallback.EVENT.register(BotManager::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(BotManager::tick);
        LOGGER.info("Carpet Players Mod loaded!");
    }
}
