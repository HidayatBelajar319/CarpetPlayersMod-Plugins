package com.carpetplayers.bot;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BotPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static final List<BotData> savedBots = new ArrayList<>();

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("carpetplayers-bots.json");
        load();
    }

    public static void saveBots() {
        savedBots.clear();
        for (BotBrain brain : BotManager.BRAINS.values()) {
            EntityPlayerMPFake bot = brain.getBot();
            if (!bot.isAlive()) continue;
            BotData data = new BotData();
            data.name = brain.getBotName();
            data.x = bot.getX();
            data.y = bot.getY();
            data.z = bot.getZ();
            data.yaw = bot.yRot;
            data.pitch = bot.xRot;
            data.state = brain.getState().name();
            data.dimension = bot.getLevel().dimension().location().toString();
            data.ownerUuid = brain.getOwnerUuid() != null ? brain.getOwnerUuid().toString() : null;
            savedBots.add(data);
        }
        try {
            File parent = configPath.getParent() != null ? configPath.getParent().toFile() : null;
            if (parent != null) Files.createDirectories(parent.toPath());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(savedBots, writer);
            }
        } catch (Exception e) {
            System.err.println("[CarpetPlayers] Failed to save bot configs: " + e.getMessage());
        }
    }

    public static void loadBots(MinecraftServer server) {
        if (!ModConfig.instance.persistentBots || savedBots.isEmpty()) return;
        for (BotData data : savedBots) {
            try {
                ResourceKey<Level> dimension = parseDimension(data.dimension);
                EntityPlayerMPFake fake = EntityPlayerMPFake.createFake(
                        data.name, server, data.x, data.y, data.z,
                        data.yaw, data.pitch, dimension, GameType.SURVIVAL, false);
                if (fake == null) continue;
                BotManager.BOTS.put(fake.getUUID(), fake);
                BotBrain brain = new BotBrain(fake);
                if (data.ownerUuid != null) {
                    try {
                        brain.setOwnerUuid(UUID.fromString(data.ownerUuid));
                    } catch (IllegalArgumentException ignored) {}
                }
                try {
                    brain.aiSetState(BotBrain.BotState.valueOf(data.state));
                } catch (IllegalArgumentException ignored) {}
                BotManager.BRAINS.put(fake.getUUID(), brain);
            } catch (Exception e) {
                System.err.println("[CarpetPlayers] Failed to restore bot " + data.name + ": " + e.getMessage());
            }
        }
        savedBots.clear();
    }

    private static ResourceKey<Level> parseDimension(String dimStr) {
        if (dimStr == null || dimStr.isEmpty()) return Level.OVERWORLD;
        try {
            ResourceLocation loc = new ResourceLocation(dimStr);
            return ResourceKey.create(Registry.DIMENSION_REGISTRY, loc);
        } catch (Exception e) {
            return Level.OVERWORLD;
        }
    }

    private static void load() {
        if (!Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<List<BotData>>(){}.getType();
            List<BotData> data = GSON.fromJson(reader, type);
            if (data != null) savedBots.addAll(data);
        } catch (Exception e) {
            System.err.println("[CarpetPlayers] Failed to load bot configs: " + e.getMessage());
        }
    }

    public static List<BotData> getSavedBots() { return Collections.unmodifiableList(savedBots); }

    public static class BotData {
        public String name;
        public double x, y, z;
        public float yaw, pitch;
        public String state;
        public String dimension;
        public String ownerUuid;
    }

    private BotPersistence() {}
}