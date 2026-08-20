package com.carpetplayers;

import com.carpetplayers.ai.AIController;
import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.bot.BotPersistence;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.rank.RankManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarpetPlayersMod implements ModInitializer {
    public static final String MOD_ID = "carpetplayers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Detect server type
        String envType = FabricLoader.getInstance().getEnvironmentType().name();
        LOGGER.info("Carpet Players Mod initializing... Environment: {}", envType);

        // Load/create the config file immediately at startup
        ModConfig.ensureLoaded();
        RankManager.init();
        BotPersistence.init();
        AIProviderManager.instance().ensureLoaded();

        // Register commands and tick handler
        CommandRegistrationCallback.EVENT.register(BotManager::registerCommands);
        // Load persistent bots on first server tick, then tick bots each tick
        final boolean[] loaded = {false};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!loaded[0]) {
                loaded[0] = true;
                BotManager.loadPersistentBots(server);
            }
            BotManager.tick(server);
        });
        com.carpetplayers.network.ServerNetworking.init();

        // Register shutdown hook to clean up AI executor and save bot configs
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BotPersistence.saveBots();
            AIController.shutdown();
            AIProviderManager.instance().shutdown();
            LOGGER.info("Carpet Players Mod shut down.");
        }, "carpetplayers-shutdown"));

        LOGGER.info("Carpet Players Mod loaded successfully!");
    }
}
