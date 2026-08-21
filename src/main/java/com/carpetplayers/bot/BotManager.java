package com.carpetplayers.bot;

import carpet.patches.EntityPlayerMPFake;
import com.carpetplayers.CarpetPlayersMod;
import com.carpetplayers.ai.AICommands;
import com.carpetplayers.ai.MinecraftToolManager;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.rank.Rank;
import com.carpetplayers.rank.RankManager;
import com.mojang.brigadier.CommandDispatcher;import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManager {
    public static final Map<UUID, EntityPlayerMPFake> BOTS = new ConcurrentHashMap<>();
    public static final Map<UUID, BotBrain> BRAINS = new ConcurrentHashMap<>();
    public static final Map<UUID, EntityPlayerMPFake> CONTROLLED = new ConcurrentHashMap<>();

    private static int nameCounter = 0;
    private static long autoSaveCounter = 0;

    /** Default bot name pool for cycling when spawning multiple bots */
    private static final String[] DEFAULT_NAMES = {"Alex", "Steve", "Herobrine", "Notch", "Dream"};

    private BotManager() {}

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        dispatcher.register(
                Commands.literal("carpetplayers")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .executes(BotManager::spawn)))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(BotManager::spawn)))
                        .then(Commands.literal("useitem")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setUseItem(context))))
                        .then(Commands.literal("interactive")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setInteractive(context))))
                        .then(Commands.literal("pvp")
                                .then(tapCommand("w-tap"))
                                .then(tapCommand("a-tap"))
                                .then(tapCommand("s-tap"))
                                .then(tapCommand("d-tap"))
                                .then(Commands.literal("multipleweapons")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setMultiWeapon(context))))
                                .then(Commands.literal("spawn")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                        .executes(BotManager::spawnPvp)))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .executes(BotManager::spawnPvp))))
                        .then(Commands.literal("ai")
                                .then(Commands.literal("start").executes(AICommands::start))
                                .then(Commands.literal("stop").executes(AICommands::stop))
                                .then(Commands.literal("status").executes(AICommands::status))
                                .then(Commands.literal("reload").executes(AICommands::reload))
                                .then(Commands.literal("test").executes(AICommands::test))
                                .then(Commands.literal("act")
                                        .then(Commands.argument("botname", StringArgumentType.word())
                                                .then(Commands.argument("instruction", StringArgumentType.greedyString())
                                                        .executes(AICommands::act))))
                                .then(Commands.literal("chat")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(AICommands::chat)))
                                .then(Commands.literal("forget")
                                        .then(Commands.argument("botname", StringArgumentType.word())
                                                .executes(AICommands::forget)))
                                .then(Commands.literal("defensive")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(AICommands::defensive)))
                                .then(Commands.literal("provider")
                                        .then(providerBranch("openai"))
                                        .then(providerBranch("gemini"))
                                        .then(providerBranch("openrouter"))
                                        .then(providerBranch("groq"))
                                        .then(providerBranch("local")))
                        )
                        .then(Commands.literal("menu")
                                .executes(BotManager::openMenu))
                        .then(Commands.literal("control")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(getBotNames(), builder))
                                        .executes(BotManager::control)))
                        .then(Commands.literal("release")
                                .executes(BotManager::release))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(getBotNames(), builder))
                                        .executes(BotManager::remove)))
                        .then(Commands.literal("list")
                                .executes(BotManager::list))
                        .then(Commands.literal("kit")
                                .then(Commands.argument("botname", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(getBotNames(), b))
                                        .then(Commands.literal("netherite_crystal").executes(ctx -> applyKit(ctx, "netherite_crystal")))
                                        .then(Commands.literal("diamond_crystal").executes(ctx -> applyKit(ctx, "diamond_crystal")))
                                        .then(Commands.literal("netherite_pot").executes(ctx -> applyKit(ctx, "netherite_pot")))
                                        .then(Commands.literal("diamond_pot").executes(ctx -> applyKit(ctx, "diamond_pot")))
                                        .then(Commands.literal("netherite_basic").executes(ctx -> applyKit(ctx, "netherite_basic")))
                                        .then(Commands.literal("diamond_basic").executes(ctx -> applyKit(ctx, "diamond_basic")))
                                )
                        )
                        .then(Commands.literal("record")
                                .then(Commands.literal("start")
                                        .then(Commands.argument("botname", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(getBotNames(), b))
                                                .executes(BotManager::recordStart)))
                                .then(Commands.literal("stop")
                                        .executes(BotManager::recordStop))
                                .then(Commands.literal("save")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(BotManager::recordSave)))
                                .then(Commands.literal("load")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(BotRecorder.listRecordings(), b))
                                                .executes(BotManager::recordLoad)))
                                .then(Commands.literal("play")
                                        .then(Commands.argument("botname", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(getBotNames(), b))
                                                .executes(BotManager::recordPlay)))
                                .then(Commands.literal("list")
                                        .executes(BotManager::recordList))
                        )
                        .then(Commands.literal("rank")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .then(Commands.argument("rank", StringArgumentType.word())
                                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                                Arrays.asList("admin", "moderator", "user"), b))
                                                        .executes(BotManager::rankSet))))
                                .then(Commands.literal("list").executes(BotManager::rankList))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .executes(BotManager::rankRemove)))
                                .then(Commands.literal("default")
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.asList("admin", "moderator", "user"), b))
                                                .executes(BotManager::rankDefault)))
                        )
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    com.carpetplayers.config.ModConfig.ensureLoaded();
                                    com.carpetplayers.ai.AIProviderManager.instance().reload();
                                    com.carpetplayers.rank.RankManager.init();
                                    com.carpetplayers.bot.BotPersistence.loadBots(server);
                                    ctx.getSource().sendSuccess(
                                            new TextComponent("[CarpetPlayers] Configs reloaded!"), false);
                                    return 1;
                                })
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tapCommand(String tapName) {
        return Commands.literal(tapName)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> setTap(context, tapName)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> providerBranch(String type) {
        return Commands.literal(type)
                .then(Commands.argument("apikey", StringArgumentType.greedyString())
                        .executes(context -> AICommands.providerKey(context, type)));
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = IntegerArgumentType.getInteger(context, "count");
        String firstName = null;
        try {
            firstName = StringArgumentType.getString(context, "name");
        } catch (Exception ignored) {
            // name argument is optional
        }
        if (ModConfig.instance.rankSystemEnabled) {
            int rankMax = RankManager.getMaxBots(player.getUUID());
            if (rankMax == 0) {
                context.getSource().sendFailure(new TextComponent("Your rank does not allow spawning bots"));
                return 0;
            }
            int effectiveMax = rankMax > 0 ? Math.min(rankMax, ModConfig.instance.maxBots) : ModConfig.instance.maxBots;
            count = Math.min(count, effectiveMax - BOTS.size());
        } else {
            count = Math.min(count, ModConfig.instance.maxBots - BOTS.size());
        }
        if (count <= 0) {
            context.getSource().sendFailure(new TextComponent(
                    "Cannot spawn more bots: maximum " + ModConfig.instance.maxBots + " reached"));
            return 0;
        }
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.getLevel().dimension();
        List<String> spawned = spawnBots(context.getSource().getServer(), count, pos, dimension,
                player.yRot, player.xRot, false, player.getUUID(), firstName);
        if (spawned.isEmpty()) {
            context.getSource().sendFailure(new TextComponent("Could not spawn any bots"));
            return 0;
        }
        context.getSource().sendSuccess(new TextComponent(
                "Spawned " + spawned.size() + " bot(s): " + String.join(", ", spawned)), true);
        return spawned.size();
    }

    private static int spawnPvp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int count = IntegerArgumentType.getInteger(context, "count");
        String firstName = null;
        try {
            firstName = StringArgumentType.getString(context, "name");
        } catch (Exception ignored) {
            // name argument is optional
        }
        if (ModConfig.instance.rankSystemEnabled) {
            int rankMax = RankManager.getMaxBots(player.getUUID());
            if (rankMax == 0) {
                context.getSource().sendFailure(new TextComponent("Your rank does not allow spawning bots"));
                return 0;
            }
            int effectiveMax = rankMax > 0 ? Math.min(rankMax, ModConfig.instance.maxBots) : ModConfig.instance.maxBots;
            count = Math.min(count, effectiveMax - BOTS.size());
        } else {
            count = Math.min(count, ModConfig.instance.maxBots - BOTS.size());
        }
        if (count <= 0) {
            context.getSource().sendFailure(new TextComponent(
                    "Cannot spawn more bots: maximum " + ModConfig.instance.maxBots + " reached"));
            return 0;
        }
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.getLevel().dimension();
        List<String> spawned = spawnBots(context.getSource().getServer(), count, pos, dimension,
                player.yRot, player.xRot, true, player.getUUID(), firstName);
        if (spawned.isEmpty()) {
            context.getSource().sendFailure(new TextComponent("Could not spawn any PvP bots"));
            return 0;
        }
        context.getSource().sendSuccess(new TextComponent(
                "Spawned " + spawned.size() + " PvP bot(s): " + String.join(", ", spawned)), true);
        return spawned.size();
    }

    private static List<String> spawnBots(MinecraftServer server, int count, Vec3 pos,
                                          ResourceKey<Level> dimension, float yaw, float pitch, boolean pvp,
                                          UUID ownerUuid, String firstName) {
        List<String> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name;
            if (i == 0 && firstName != null && !firstName.isEmpty()) {
                // First bot uses the user-specified name
                name = firstName;
                // Handle name collision
                if (isNameTaken(server, name)) {
                    int suffix = 2;
                    while (isNameTaken(server, name + suffix)) {
                        suffix++;
                    }
                    name = name + suffix;
                }
            } else {
                // Subsequent bots cycle through defaults: player name, Alex, Steve, etc.
                name = nextName(server);
            }
            EntityPlayerMPFake fake = EntityPlayerMPFake.createFake(name, server, pos.x, pos.y, pos.z,
                    yaw, pitch, dimension, GameType.SURVIVAL, false);
            if (fake == null) {
                continue;
            }
            if (pvp) {
                PvPBot.equip(fake);
            }
            BOTS.put(fake.getUUID(), fake);
            BotBrain brain = pvp ? new PvPBot(fake) : new BotBrain(fake);
            brain.setOwnerUuid(ownerUuid);
            BRAINS.put(fake.getUUID(), brain);
            CarpetPlayersMod.LOGGER.info("Spawned bot {} at ({}, {}, {})", name, pos.x, pos.y, pos.z);
            spawned.add(name);
        }
        return spawned;
    }

    private static int setUseItem(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig.instance.useItemEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(new TextComponent("Bot item usage set to " + enabled), true);
        return 1;
    }

    private static int setInteractive(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig.instance.interactiveEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(new TextComponent("Bot interactive mode set to " + enabled), true);
        return 1;
    }

    private static int setMultiWeapon(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig.instance.multiWeaponEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(new TextComponent("Bot multi-weapon system set to " + enabled), true);
        return 1;
    }

    private static int setTap(CommandContext<CommandSourceStack> context, String tapName) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig.instance.setTap(tapName, enabled);
        ModConfig.save();
        context.getSource().sendSuccess(new TextComponent(
                "Tap-hit control " + tapName + " set to " + enabled), true);
        return 1;
    }

    private static int control(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer controller = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        EntityPlayerMPFake bot = findBotByName(name);
        if (bot == null) {
            context.getSource().sendFailure(new TextComponent("Bot '" + name + "' not found"));
            return 0;
        }
        CONTROLLED.put(controller.getUUID(), bot);
        context.getSource().sendSuccess(new TextComponent(
                "Now controlling " + bot.getName().getString() + ". Type /carpetplayers release to stop."), true);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer controller = context.getSource().getPlayerOrException();
        EntityPlayerMPFake bot = CONTROLLED.remove(controller.getUUID());
        if (bot == null) {
            context.getSource().sendFailure(new TextComponent("You are not controlling any bot"));
            return 0;
        }
        BotBrain brain = BRAINS.get(bot.getUUID());
        if (brain != null) {
            brain.actions().stopMovement();
        }
        context.getSource().sendSuccess(new TextComponent(
                "Released control of " + bot.getName().getString()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        EntityPlayerMPFake bot = findBotByName(name);
        if (bot == null) {
            context.getSource().sendFailure(new TextComponent("Bot '" + name + "' not found"));
            return 0;
        }
        removeBot(bot);
        context.getSource().sendSuccess(new TextComponent("Removed bot '" + name + "'"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        if (BOTS.isEmpty()) {
            context.getSource().sendSuccess(new TextComponent("No bots are active"), false);
            return 0;
        }
        List<String> names = new ArrayList<>();
        for (EntityPlayerMPFake bot : BOTS.values()) {
            names.add(bot.getName().getString());
        }
        context.getSource().sendSuccess(new TextComponent(
                "Active bots (" + names.size() + "): " + String.join(", ", names)), false);
        return names.size();
    }

    private static int applyKit(CommandContext<CommandSourceStack> context, String kitName) {
        String botName = StringArgumentType.getString(context, "botname");
        BotBrain brain = MinecraftToolManager.findBotByName(botName);
        if (brain == null) {
            context.getSource().sendFailure(new TextComponent("[Kit] Bot not found: " + botName));
            return 0;
        }
        boolean ok = KitManager.applyKit(brain, kitName);
        context.getSource().sendSuccess(new TextComponent(
                ok ? "[Kit] " + kitName + " equipped on " + botName
                        : "[Kit] Unknown kit: " + kitName), false);
        return ok ? 1 : 0;
    }

    private static int recordStart(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botname");
        if (BotRecorder.startRecording(botName)) {
            context.getSource().sendSuccess(
                    new TextComponent("[Record] Recording started for '" + botName + "'"), true);
            return 1;
        }
        context.getSource().sendFailure(new TextComponent(
                "[Record] Could not start recording (bot not found or already recording)"));
        return 0;
    }

    private static int recordStop(CommandContext<CommandSourceStack> context) {
        if (BotRecorder.stopRecording()) {
            context.getSource().sendSuccess(new TextComponent(
                    "[Record] Recording stopped (" + BotRecorder.getFrameCount() + " frames captured)"), true);
            return 1;
        }
        context.getSource().sendFailure(new TextComponent("[Record] Not currently recording"));
        return 0;
    }

    private static int recordSave(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (BotRecorder.getFrameCount() == 0) {
            context.getSource().sendFailure(new TextComponent("[Record] No frames to save"));
            return 0;
        }
        BotRecorder.saveRecording(name);
        context.getSource().sendSuccess(new TextComponent(
                "[Record] Saved recording '" + name + "' (" + BotRecorder.getFrameCount() + " frames)"), true);
        return 1;
    }

    private static int recordLoad(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (BotRecorder.loadRecording(name)) {
            context.getSource().sendSuccess(new TextComponent(
                    "[Record] Loaded recording '" + name + "' (" + BotRecorder.getFrameCount() + " frames)"), true);
            return 1;
        }
        context.getSource().sendFailure(new TextComponent("[Record] Recording '" + name + "' not found"));
        return 0;
    }

    private static int recordPlay(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botname");
        if (BotRecorder.startPlayback(botName)) {
            context.getSource().sendSuccess(new TextComponent(
                    "[Record] Playing recording on '" + botName + "'"), true);
            return 1;
        }
        context.getSource().sendFailure(new TextComponent(
                "[Record] Could not start playback (no recording loaded, already playing, or bot not found)"));
        return 0;
    }

    private static int recordList(CommandContext<CommandSourceStack> context) {
        List<String> recordings = BotRecorder.listRecordings();
        if (recordings.isEmpty()) {
            context.getSource().sendSuccess(new TextComponent("[Record] No saved recordings"), false);
            return 0;
        }
        context.getSource().sendSuccess(new TextComponent(
                "[Record] Saved recordings: " + String.join(", ", recordings)), false);
        return recordings.size();
    }

    public static void removeBot(EntityPlayerMPFake bot) {
        BRAINS.remove(bot.getUUID());
        BOTS.remove(bot.getUUID());
        CONTROLLED.values().remove(bot);
        if (bot.isAlive()) {
            bot.kill();
        }
        CarpetPlayersMod.LOGGER.info("Removed bot {}", bot.getName().getString());
    }

    public static void tick(MinecraftServer server) {
        // Recording & Playback ticks
        BotRecorder.recordTick();
        BotRecorder.playbackTick();

        autoSaveCounter++;
        if (ModConfig.instance.persistentBots && autoSaveCounter >= ModConfig.instance.autoSaveIntervalMinutes * 20L * 60L) {
            autoSaveCounter = 0;
            BotPersistence.saveBots();
        }
        ModConfig.ensureLoaded();
        if (BOTS.isEmpty() && CONTROLLED.isEmpty()) {
            return;
        }
        List<UUID> dead = new ArrayList<>();
        for (BotBrain brain : BRAINS.values()) {
            if (!brain.getBot().isAlive()) {
                dead.add(brain.getUuid());
            } else {
                brain.tick();
            }
        }
        for (UUID uuid : dead) {
            BOTS.remove(uuid);
            BRAINS.remove(uuid);
        }
        for (Map.Entry<UUID, EntityPlayerMPFake> entry : CONTROLLED.entrySet()) {
            ServerPlayer controller = server.getPlayerList().getPlayer(entry.getKey());
            if (controller == null) {
                CONTROLLED.remove(entry.getKey());
                continue;
            }
            BotBrain brain = BRAINS.get(entry.getValue().getUUID());
            if (brain != null) {
                brain.tickControlled(controller);
            }
        }
    }

    public static void loadPersistentBots(MinecraftServer server) {
        BotPersistence.loadBots(server);
    }

    private static String nextName(MinecraftServer server) {
        // Cycle through default names, then fall back to numbered names
        if (nameCounter < DEFAULT_NAMES.length) {
            String name = DEFAULT_NAMES[nameCounter];
            if (!isNameTaken(server, name)) {
                nameCounter++;
                return name;
            }
        }
        // Fallback: numbered names
        String name;
        do {
            nameCounter++;
            name = "Bot_" + nameCounter;
        } while (isNameTaken(server, name));
        return name;
    }

    private static boolean isNameTaken(MinecraftServer server, String name) {
        if (server.getPlayerList().getPlayerByName(name) != null) {
            return true;
        }
        return findBotByName(name) != null;
    }

    private static EntityPlayerMPFake findBotByName(String name) {
        for (EntityPlayerMPFake bot : BOTS.values()) {
            if (bot.getName().getString().equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    public static BotBrain findBrainByName(String name) {
        for (BotBrain brain : BRAINS.values()) {
            if (brain.getBotName().equalsIgnoreCase(name)
                    || brain.getBot().getName().getString().equalsIgnoreCase(name)) {
                return brain;
            }
        }
        return null;
    }

    public static List<String> getBotNames() {
        List<String> names = new ArrayList<>();
        for (EntityPlayerMPFake bot : BOTS.values()) {
            names.add(bot.getName().getString());
        }
        return names;
    }

    /**
     * Returns true if the server is running as a dedicated server (multiplayer).
     * Returns false if running as an integrated server (singleplayer).
     */
    public static boolean isDedicated(MinecraftServer server) {
        return server != null && !server.isSingleplayer();
    }

    private static int openMenu(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            com.carpetplayers.network.ServerNetworking.openMenuFor(player);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    private static int rankSet(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        String rankName = StringArgumentType.getString(context, "rank");
        Rank rank = Rank.fromName(rankName);

        MinecraftServer server = context.getSource().getServer();
        ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);
        UUID targetUuid = target != null ? target.getUUID() : null;

        if (targetUuid == null) {
            context.getSource().sendFailure(new TextComponent("Player '" + playerName + "' not found online"));
            return 0;
        }

        RankManager.setRank(targetUuid, rank);
        context.getSource().sendSuccess(new TextComponent(
                "Set rank of " + playerName + " to " + rank.getName()), true);
        if (target != null) {
            target.sendMessage(new TextComponent("Your rank has been set to " + rank.getName()), target.getUUID());
        }
        return 1;
    }

    private static int rankList(CommandContext<CommandSourceStack> context) {
        Map<UUID, Rank> allRanks = RankManager.getAllRanks();
        if (allRanks.isEmpty()) {
            context.getSource().sendSuccess(new TextComponent("No ranks assigned. Default: " + RankManager.getDefaultRank().getName()), false);
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        StringBuilder sb = new StringBuilder("Ranks:");
        for (Map.Entry<UUID, Rank> entry : allRanks.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            String name = p != null ? p.getName().getString() : entry.getKey().toString().substring(0, 8) + "...";
            sb.append("\n  ").append(name).append(" -> ").append(entry.getValue().getName());
        }
        sb.append("\nDefault: ").append(RankManager.getDefaultRank().getName());
        context.getSource().sendSuccess(new TextComponent(sb.toString()), false);
        return 1;
    }

    private static int rankRemove(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);

        if (target == null) {
            context.getSource().sendFailure(new TextComponent("Player '" + playerName + "' not found online"));
            return 0;
        }

        RankManager.removeRank(target.getUUID());
        context.getSource().sendSuccess(new TextComponent(
                "Removed rank for " + playerName + " (now: " + RankManager.getDefaultRank().getName() + ")"), true);
        return 1;
    }

    private static int rankDefault(CommandContext<CommandSourceStack> context) {
        String rankName = StringArgumentType.getString(context, "rank");
        Rank rank = Rank.fromName(rankName);
        RankManager.setDefaultRank(rank);
        context.getSource().sendSuccess(new TextComponent("Default rank set to " + rank.getName()), true);
        return 1;
    }
}
