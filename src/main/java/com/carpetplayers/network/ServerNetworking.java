package com.carpetplayers.network;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.bot.BotBrain;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.bot.KitManager;
import com.carpetplayers.bot.PvPBot;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.waypoint.Waypoint;
import com.carpetplayers.waypoint.WaypointManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ServerNetworking {

    private static int nameCounter = 0;

    private ServerNetworking() {
    }

    public static void init() {
        // Client -> Server: bot control actions
        // IMPORTANT: Copy buffer before server.execute() — Fabric releases it after callback returns
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.BOT_ACTION,
                (MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
                 FriendlyByteBuf buf, PacketSender responseSender) -> {
                    String action = buf.readUtf();
                    FriendlyByteBuf copy = PacketByteBufs.create();
                    if (buf.isReadable()) {
                        copy.writeBytes(buf);
                    }
                    server.execute(() -> handleBotAction(server, player, copy, action));
                });

        // Client -> Server: request the active bot list
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQUEST_BOTS,
                (MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
                 FriendlyByteBuf buf, PacketSender responseSender) -> {
                    server.execute(() -> sendBotList(player));
                });

        // Client -> Server: death waypoint report
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.DEATH_WAYPOINT,
                (MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
                 FriendlyByteBuf buf, PacketSender responseSender) -> {
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();
                    String dimStr = buf.readUtf();
                    FriendlyByteBuf copy = PacketByteBufs.create();
                    copy.writeDouble(x);
                    copy.writeDouble(y);
                    copy.writeDouble(z);
                    copy.writeUtf(dimStr);
                    server.execute(() -> handleDeathWaypoint(server, player, copy));
                });
    }

    // Server -> Client: send the current bot list to the requesting player
    public static void sendBotList(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(BotManager.BOTS.size());
        for (EntityPlayerMPFake bot : BotManager.BOTS.values()) {
            buf.writeUtf(bot.getName().getString());
            buf.writeFloat(bot.getHealth());
            BotBrain brain = BotManager.BRAINS.get(bot.getUUID());
            buf.writeUtf(brain != null ? brain.getState().name() : "WANDER");
            buf.writeBoolean(brain instanceof PvPBot);
        }
        ServerPlayNetworking.send(player, ModPackets.BOT_LIST, buf);
    }

    // Server -> Client: tell the client to open the controller menu
    public static void openMenuFor(ServerPlayer player) {
        ServerPlayNetworking.send(player, ModPackets.OPEN_MENU, PacketByteBufs.create());
    }

    private static void handleBotAction(MinecraftServer server, ServerPlayer player,
                                        FriendlyByteBuf buf, String action) {
        switch (action) {
            case "spawn": {
                int count = buf.isReadable() ? buf.readInt() : 1;
                spawnBots(server, player, count, false);
                break;
            }
            case "spawn_pvp": {
                int count = buf.isReadable() ? buf.readInt() : 1;
                spawnBots(server, player, count, true);
                break;
            }
            case "remove": {
                String botname = buf.readUtf();
                EntityPlayerMPFake bot = findBotByName(botname);
                if (bot == null) {
                    player.sendMessage(new TextComponent("Bot '" + botname + "' not found"), player.getUUID());
                } else {
                    BotManager.removeBot(bot);
                    player.sendMessage(new TextComponent("Removed bot '" + botname + "'"), player.getUUID());
                }
                break;
            }
            case "kit": {
                String botname = buf.readUtf();
                String kitname = buf.readUtf();
                BotBrain brain = findBrainByName(botname);
                if (brain == null) {
                    player.sendMessage(new TextComponent("[Kit] Bot not found: " + botname), player.getUUID());
                } else {
                    boolean ok = KitManager.applyKit(brain, kitname);
                    player.sendMessage(new TextComponent(
                            ok ? "[Kit] " + kitname + " equipped on " + botname
                                    : "[Kit] Unknown kit: " + kitname), player.getUUID());
                }
                break;
            }
            case "control": {
                String botname = buf.readUtf();
                EntityPlayerMPFake bot = findBotByName(botname);
                if (bot == null) {
                    player.sendMessage(new TextComponent("Bot '" + botname + "' not found"), player.getUUID());
                } else {
                    BotManager.CONTROLLED.put(player.getUUID(), bot);
                    player.sendMessage(new TextComponent(
                            "Now controlling " + bot.getName().getString() + ". Use 'release' to stop."),
                            player.getUUID());
                }
                break;
            }
            case "release": {
                EntityPlayerMPFake bot = BotManager.CONTROLLED.remove(player.getUUID());
                if (bot != null) {
                    player.sendMessage(new TextComponent("Released control of " + bot.getName().getString()),
                            player.getUUID());
                }
                break;
            }
            case "set_state": {
                String botname = buf.readUtf();
                String state = buf.readUtf();
                BotBrain brain = findBrainByName(botname);
                if (brain == null) {
                    player.sendMessage(new TextComponent("Bot '" + botname + "' not found"), player.getUUID());
                } else {
                    try {
                        BotBrain.BotState parsed = BotBrain.BotState.valueOf(state.toUpperCase());
                        brain.aiSetState(parsed);
                        player.sendMessage(new TextComponent("Bot state -> " + parsed.name()), player.getUUID());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(new TextComponent("Invalid state: " + state), player.getUUID());
                    }
                }
                break;
            }
            case "toggle_useitem": {
                ModConfig.instance.useItemEnabled = buf.readBoolean();
                ModConfig.save();
                break;
            }
            case "toggle_interactive": {
                ModConfig.instance.interactiveEnabled = buf.readBoolean();
                ModConfig.save();
                break;
            }
            case "toggle_multiweapon": {
                ModConfig.instance.multiWeaponEnabled = buf.readBoolean();
                ModConfig.save();
                break;
            }
            default:
                break;
        }
    }

    private static void handleDeathWaypoint(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        if (!ModConfig.instance.deathWaypointEnabled) return;

        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String dimStr = buf.readUtf();
        ResourceKey<Level> dimKey = WaypointManager.stringToDimension(dimStr);

        Waypoint wp = WaypointManager.handleDeath(player.getUUID(), x, y, z, dimKey);
        if (wp != null) {
            // Notify the player
            player.sendMessage(
                    new TextComponent("[Waypoint] " + wp.getName() + " created at " + wp.coordString()),
                    player.getUUID());
        }
    }

    private static void spawnBots(MinecraftServer server, ServerPlayer player, int count, boolean pvp) {
        count = Math.min(count, ModConfig.instance.maxBots - BotManager.BOTS.size());
        if (count <= 0) {
            player.sendMessage(new TextComponent(
                    "Cannot spawn more bots: maximum " + ModConfig.instance.maxBots + " reached"), player.getUUID());
            return;
        }
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.getLevel().dimension();
        List<String> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = nextBotName(server);
            EntityPlayerMPFake fake = EntityPlayerMPFake.createFake(name, server, pos.x, pos.y, pos.z,
                    player.yRot, player.xRot, dimension, GameType.SURVIVAL, false);
            if (fake == null) {
                continue;
            }
            if (pvp) {
                PvPBot.equip(fake);
            }
            BotManager.BOTS.put(fake.getUUID(), fake);
            BotBrain brain = pvp ? new PvPBot(fake) : new BotBrain(fake);
            brain.setOwnerUuid(player.getUUID());
            BotManager.BRAINS.put(fake.getUUID(), brain);
            spawned.add(name);
        }
        if (spawned.isEmpty()) {
            player.sendMessage(new TextComponent("Could not spawn any " + (pvp ? "PvP " : "") + "bots"),
                    player.getUUID());
            return;
        }
        player.sendMessage(new TextComponent(
                "Spawned " + spawned.size() + (pvp ? " PvP " : " ") + "bot(s): " + String.join(", ", spawned)),
                player.getUUID());
    }

    private static final String[] DEFAULT_NAMES = {"Alex", "Steve", "Herobrine", "Notch", "Dream"};

    private static String nextBotName(MinecraftServer server) {
        if (nameCounter < DEFAULT_NAMES.length) {
            String name = DEFAULT_NAMES[nameCounter];
            if (server.getPlayerList().getPlayerByName(name) == null
                    && !BotManager.getBotNames().contains(name)) {
                nameCounter++;
                return name;
            }
        }
        String name;
        do {
            nameCounter++;
            name = "Bot_" + nameCounter;
        } while (server.getPlayerList().getPlayerByName(name) != null
                || BotManager.getBotNames().contains(name));
        return name;
    }

    private static EntityPlayerMPFake findBotByName(String name) {
        for (EntityPlayerMPFake bot : BotManager.BOTS.values()) {
            if (bot.getName().getString().equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    private static BotBrain findBrainByName(String name) {
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (brain.getBotName().equalsIgnoreCase(name)) {
                return brain;
            }
        }
        return null;
    }
}
