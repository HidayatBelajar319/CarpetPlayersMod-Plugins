package com.carpetplayers;

import com.carpetplayers.ai.AIController;
import com.carpetplayers.ai.AIProviderManager;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.bot.BotPersistence;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.rank.RankManager;
import com.carpetplayers.waypoint.WaypointManager;
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
        WaypointManager.init();
        WaypointManager.loadAll();

        // Register commands (carpetplayers + aliases /cp, /cps)
        CommandRegistrationCallback.EVENT.register(BotManager::registerCommands);
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            // Register /cp alias
            com.carpetplayers.waypoint.WaypointCommands.registerWaypointCommands(dispatcher,
                    net.minecraft.commands.Commands.literal("cp")
                            .requires(source -> source.hasPermission(2)));
            // Register /cps alias
            com.carpetplayers.waypoint.WaypointCommands.registerWaypointCommands(dispatcher,
                    net.minecraft.commands.Commands.literal("cps")
                            .requires(source -> source.hasPermission(2)));
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                com.carpetplayers.worldedit.WorldEditCommands.registerWorldEditCommands(dispatcher));

        // Load persistent bots on first server tick, then tick bots each tick
        final boolean[] loaded = {false};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!loaded[0]) {
                loaded[0] = true;
                BotManager.loadPersistentBots(server);
            }
            BotManager.tick(server);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) {
                LOGGER.info("CarpetPlayers: Re-syncing configs after reload...");
                ModConfig.ensureLoaded();
                AIProviderManager.instance().reload();
                com.carpetplayers.rank.RankManager.init();
                BotPersistence.loadBots(server);
                LOGGER.info("CarpetPlayers: Config re-sync complete.");
            }
        });
        com.carpetplayers.network.ServerNetworking.init();

        // Register shutdown hook to clean up AI executor and save bot configs
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BotPersistence.saveBots();
            WaypointManager.saveAll();
            AIController.shutdown();
            AIProviderManager.instance().shutdown();
            LOGGER.info("Carpet Players Mod shut down.");
        }, "carpetplayers-shutdown"));

        LOGGER.info("Carpet Players Mod loaded successfully!");
    }
}
