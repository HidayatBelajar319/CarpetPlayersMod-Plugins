package com.carpetplayers.bot;

import com.carpetplayers.CarpetPlayersPlugin;
import com.carpetplayers.ai.AICommands;
import com.carpetplayers.ai.AIController;
import com.carpetplayers.ai.MinecraftToolManager;
import com.carpetplayers.config.ModConfig;
import com.carpetplayers.nms.FakePlayer;
import com.carpetplayers.nms.FakePlayerFactory;
import com.carpetplayers.rank.Rank;
import com.carpetplayers.rank.RankManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManager implements CommandExecutor, TabCompleter, Listener {

    public static final Map<UUID, FakePlayer> BOTS = new ConcurrentHashMap<>();
    public static final Map<UUID, BotBrain> BRAINS = new ConcurrentHashMap<>();
    public static final Map<UUID, FakePlayer> CONTROLLED = new ConcurrentHashMap<>();

    private static int nameCounter = 0;
    private static BotManager instance;

    private BotManager() {
    }

    public static void registerCommands(org.bukkit.plugin.java.JavaPlugin plugin) {
        instance = new BotManager();
        plugin.getCommand("carpetplayers").setExecutor(instance);
        plugin.getCommand("carpetplayers").setTabCompleter(instance);
    }

    public static void registerEvents(org.bukkit.plugin.java.JavaPlugin plugin) {
        if (instance == null) {
            instance = new BotManager();
        }
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carpetplayers.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§f/carpetplayers <spawn|pvp|ai|control|release|remove|list|kit|useitem|interactive|rank>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "spawn":
                return cmdSpawn(sender, args);
            case "useitem":
                return cmdSetBool(sender, args, "useitem");
            case "interactive":
                return cmdSetBool(sender, args, "interactive");
            case "pvp":
                return cmdPvp(sender, args);
            case "ai":
                return cmdAi(sender, args);
            case "control":
                return cmdControl(sender, args);
            case "release":
                return cmdRelease(sender);
            case "remove":
                return cmdRemove(sender, args);
            case "list":
                return cmdList(sender);
            case "kit":
                return cmdKit(sender, args);
            case "rank":
                return cmdRank(sender, args);
            case "protocol":
                return cmdProtocol(sender, args);
            default:
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
                return true;
        }
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return false;
        }
        return true;
    }

    /**
     * /carpetplayers protocol [player] - show the client protocol version.
     * Uses ViaVersion to detect legacy clients (e.g. 1.16.5 joining through ViaBackwards).
     */
    private boolean cmdProtocol(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cUsage: /carpetplayers protocol <player>");
            return true;
        }
        if (!com.carpetplayers.via.ViaCompat.isAvailable()) {
            sender.sendMessage("§eViaVersion is not installed - client protocol cannot be detected.");
            return true;
        }
        int protocol = com.carpetplayers.via.ViaCompat.getProtocolVersion(target);
        String name = com.carpetplayers.via.ViaCompat.getClientVersionName(target);
        boolean legacy = com.carpetplayers.via.ViaCompat.isLegacyClient(target);
        sender.sendMessage("§f" + target.getName() + " -> protocol §e" + protocol
                + "§f (" + name + ")" + (legacy ? " §7[legacy client/ViaBackwards]" : " §a[modern version]"));
        return true;
    }

    private boolean cmdSpawn(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /carpetplayers spawn <count>");
            return true;
        }
        try {
            int count = Integer.parseInt(args[1]);
            count = Math.max(1, Math.min(100, count));
            Player player = (Player) sender;
            if (ModConfig.instance.rankSystemEnabled) {
                int rankMax = RankManager.getMaxBots(player.getUniqueId());
                if (rankMax == 0) {
                    sender.sendMessage("§cYour rank does not allow spawning bots");
                    return true;
                }
                count = Math.min(count, rankMax);
            }
            List<String> spawned = spawnBots(player, count, false);
            if (spawned.isEmpty()) {
                sender.sendMessage("§cCannot spawn bots: maximum " + ModConfig.instance.maxBots + " reached");
            } else {
                sender.sendMessage("§aSpawned " + spawned.size() + " bot(s): " + String.join(", ", spawned));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cThe count must be a number.");
        }
        return true;
    }

    private boolean cmdPvp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§fCarpet PvP: w-tap/a-tap/s-tap/d-tap/multipleweapons/spawn <count>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "spawn": {
                if (!requirePlayer(sender)) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers pvp spawn <count>");
                    return true;
                }
                try {
                    int count = Integer.parseInt(args[2]);
                    count = Math.max(1, Math.min(100, count));
                    if (ModConfig.instance.rankSystemEnabled) {
                        int rankMax = RankManager.getMaxBots(((Player) sender).getUniqueId());
                        if (rankMax == 0) {
                            sender.sendMessage("§cYour rank does not allow spawning bots");
                            return true;
                        }
                        count = Math.min(count, rankMax);
                    }
                    List<String> spawned = spawnBots((Player) sender, count, true);
                    if (spawned.isEmpty()) {
                        sender.sendMessage("§cCannot spawn PvP bots: maximum " + ModConfig.instance.maxBots + " reached");
                    } else {
                        sender.sendMessage("§aSpawned " + spawned.size() + " PvP bot(s): " + String.join(", ", spawned));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cThe count must be a number.");
                }
                return true;
            }
            case "multipleweapons": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers pvp multipleweapons <true|false>");
                    return true;
                }
                boolean enabled = Boolean.parseBoolean(args[2]);
                ModConfig.instance.multiWeaponEnabled = enabled;
                ModConfig.save();
                sender.sendMessage("§aBot multi-weapon system set to " + enabled);
                return true;
            }
            case "w-tap":
            case "a-tap":
            case "s-tap":
            case "d-tap": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers pvp " + args[1].toLowerCase() + " <true|false>");
                    return true;
                }
                boolean enabled = Boolean.parseBoolean(args[2]);
                ModConfig.instance.setTap(args[1].toLowerCase(), enabled);
                ModConfig.save();
                sender.sendMessage("§aTap-hit control " + args[1].toLowerCase() + " set to " + enabled);
                return true;
            }
            default:
                sender.sendMessage("§cUnknown pvp subcommand: " + args[1]);
                return true;
        }
    }

    private boolean cmdSetBool(CommandSender sender, String[] args, String name) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /carpetplayers " + name + " <true|false>");
            return true;
        }
        boolean enabled = Boolean.parseBoolean(args[1]);
        switch (name) {
            case "useitem":
                ModConfig.instance.useItemEnabled = enabled;
                break;
            case "interactive":
                ModConfig.instance.interactiveEnabled = enabled;
                break;
            default:
                return true;
        }
        ModConfig.save();
        sender.sendMessage("§aBot " + name + " set to " + enabled);
        return true;
    }

    private boolean cmdAi(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§f/carpetplayers ai <start|stop|status|reload|test|act|chat|forget|defensive|provider>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "start":
                AICommands.handleStart(sender);
                return true;
            case "stop":
                AICommands.handleStop(sender);
                return true;
            case "status":
                AICommands.handleStatus(sender);
                return true;
            case "reload":
                AICommands.handleReload(sender);
                return true;
            case "test":
                AICommands.handleTest(sender);
                return true;
            case "act":
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /carpetplayers ai act <botname> <instruction>");
                    return true;
                }
                StringBuilder instruction = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (instruction.length() > 0) {
                        instruction.append(" ");
                    }
                    instruction.append(args[i]);
                }
                AICommands.handleAct(sender, args[2], instruction.toString());
                return true;
            case "chat":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers ai chat <true|false>");
                    return true;
                }
                AICommands.handleChat(sender, Boolean.parseBoolean(args[2]));
                return true;
            case "forget":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers ai forget <botname>");
                    return true;
                }
                AICommands.handleForget(sender, args[2]);
                return true;
            case "defensive":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers ai defensive <true|false>");
                    return true;
                }
                AICommands.handleDefensive(sender, Boolean.parseBoolean(args[2]));
                return true;
            case "provider":
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /carpetplayers ai provider <openai|gemini|openrouter|groq|local> <apikey>");
                    return true;
                }
                AICommands.handleProviderKey(sender, args[2], args[3]);
                return true;
            default:
                sender.sendMessage("§cUnknown ai subcommand: " + args[1]);
                return true;
        }
    }

    private boolean cmdControl(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /carpetplayers control <bot name>");
            return true;
        }
        FakePlayer bot = findBotByName(args[1]);
        if (bot == null) {
            sender.sendMessage("§cBot '" + args[1] + "' not found");
            return true;
        }
        CONTROLLED.put(((Player) sender).getUniqueId(), bot);
        sender.sendMessage("§aYou are now controlling " + bot.getName()
                + ". Type /carpetplayers release to stop.");
        return true;
    }

    private boolean cmdRelease(CommandSender sender) {
        if (!requirePlayer(sender)) {
            return true;
        }
        FakePlayer bot = CONTROLLED.remove(((Player) sender).getUniqueId());
        if (bot == null) {
            sender.sendMessage("§cYou are not controlling any bot");
            return true;
        }
        BotBrain brain = BRAINS.get(bot.getUUID());
        if (brain != null) {
            brain.aiStop();
        }
        sender.sendMessage("§aReleased control of " + bot.getName());
        return true;
    }

    private boolean cmdRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /carpetplayers remove <bot name>");
            return true;
        }
        FakePlayer bot = findBotByName(args[1]);
        if (bot == null) {
            sender.sendMessage("§cBot '" + args[1] + "' not found");
            return true;
        }
        removeBot(bot);
        sender.sendMessage("§aBot '" + args[1] + "' removed");
        return true;
    }

    private boolean cmdList(CommandSender sender) {
        if (BOTS.isEmpty()) {
            sender.sendMessage("§fNo active bots");
            return true;
        }
        List<String> names = new ArrayList<>();
        for (FakePlayer bot : BOTS.values()) {
            names.add(bot.getName().getString());
        }
        sender.sendMessage("§fActive bots (" + names.size() + "): " + String.join(", ", names));
        return true;
    }

    private boolean cmdKit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§f/carpetplayers kit <botname> <netherite_crystal|diamond_crystal|netherite_pot|diamond_pot|netherite_basic|diamond_basic>");
            return true;
        }
        String botName = args[1];
        String kitName = args[2].toLowerCase();
        BotBrain brain = MinecraftToolManager.findBotByName(botName);
        if (brain == null) {
            sender.sendMessage("§c[Kit] Bot not found: " + botName);
            return true;
        }
        boolean ok = KitManager.applyKit(brain, kitName);
        sender.sendMessage(ok
                ? "§a[Kit] " + kitName + " equipped to " + botName
                : "§c[Kit] Unknown kit: " + kitName);
        return true;
    }

    private List<String> spawnBots(Player owner, int count, boolean pvp) {
        List<String> spawned = new ArrayList<>();
        count = Math.min(count, ModConfig.instance.maxBots - BOTS.size());
        org.bukkit.World world = owner.getWorld();
        for (int i = 0; i < count; i++) {
            String name = nextName();
            FakePlayer fake = FakePlayerFactory.spawn(name, world,
                    owner.getLocation().getX(), owner.getLocation().getY(), owner.getLocation().getZ(),
                    owner.getLocation().getYaw(), owner.getLocation().getPitch());
            if (fake == null) {
                continue;
            }
            if (pvp) {
                PvPBot.equip(fake);
            }
            BOTS.put(fake.getUUID(), fake);
            BotBrain brain = pvp ? new PvPBot(fake) : new BotBrain(fake);
            brain.setOwnerUuid(owner.getUniqueId());
            BRAINS.put(fake.getUUID(), brain);
            CarpetPlayersPlugin.log("Spawned bot " + name + " at ("
                    + owner.getLocation().getX() + ", " + owner.getLocation().getY() + ", "
                    + owner.getLocation().getZ() + ")");
            spawned.add(name);
        }
        return spawned;
    }

    public static void removeBot(FakePlayer bot) {
        BRAINS.remove(bot.getUUID());
        BOTS.remove(bot.getUUID());
        CONTROLLED.values().remove(bot);
        FakePlayerFactory.despawn(bot);
        CarpetPlayersPlugin.log("Removed bot " + bot.getName());
    }

    public static void removeAllBots() {
        for (FakePlayer bot : new ArrayList<FakePlayer>(BOTS.values())) {
            removeBot(bot);
        }
        BOTS.clear();
        BRAINS.clear();
        CONTROLLED.clear();
    }

    public static void tick() {
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
            FakePlayer bot = BOTS.remove(uuid);
            BRAINS.remove(uuid);
            if (bot != null) {
                CONTROLLED.values().remove(bot);
                FakePlayerFactory.despawn(bot);
            }
        }
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        for (Map.Entry<UUID, FakePlayer> entry : CONTROLLED.entrySet()) {
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

    private static String nextName() {
        String name;
        do {
            nameCounter++;
            name = "FriendBot_" + nameCounter;
        } while (isNameTaken(name));
        return name;
    }

    private static boolean isNameTaken(String name) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        if (server.getPlayerList().getPlayer(name) != null) {
            return true;
        }
        return findBotByName(name) != null;
    }

    public static FakePlayer findBotByName(String name) {
        for (FakePlayer bot : BOTS.values()) {
            if (bot.getName().getString().equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    private static List<String> getBotNames() {
        List<String> names = new ArrayList<>();
        for (FakePlayer bot : BOTS.values()) {
            names.add(bot.getName().getString());
        }
        return names;
    }

    // ============ Event listeners ============

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String command = event.getMessage().trim();
        if (!command.startsWith("!")) {
            return;
        }
        // Command for the bot controlled by this player: "!<botname> <command>"
        String[] parts = command.substring(1).split("\\s+", 2);
        if (parts.length < 2) {
            return;
        }
        FakePlayer bot = findBotByName(parts[0]);
        if (bot == null) {
            return;
        }
        BotBrain brain = BRAINS.get(bot.getUUID());
        if (brain == null) {
            return;
        }
        String[] cell = new String[]{parts[1]};
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(CarpetPlayersPlugin.instance,
                () -> brain.handleChatCommand(cell[0]));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBotDamaged(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        FakePlayer bot = null;
        if (event.getEntity() instanceof CraftPlayer) {
            ServerPlayer handle = ((CraftPlayer) event.getEntity()).getHandle();
            if (handle instanceof FakePlayer) {
                bot = (FakePlayer) handle;
            }
        }
        if (bot == null) {
            return;
        }
        BotBrain brain = BRAINS.get(bot.getUUID());
        if (brain == null) {
            return;
        }
        org.bukkit.entity.Entity damagerEntity = event.getDamager();
        if (damagerEntity instanceof CraftPlayer) {
            ServerPlayer attacker = ((CraftPlayer) damagerEntity).getHandle();
            brain.onAttacked(attacker);
        }
        // Defensive AI chat
        if (com.carpetplayers.ai.AIProviderManager.instance().isDefensiveEnabled()
                && com.carpetplayers.ai.AIProviderManager.instance().isEnabled()) {
            String botName = bot.getName().getString();
            Bukkit.getScheduler().runTaskAsynchronously(CarpetPlayersPlugin.instance,
                    () -> AIController.runChat(botName, "We are being attacked, defend yourself!"));
        }
    }

    // ============ Tab completer ============

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("spawn", "pvp", "ai", "control", "release", "remove", "list", "kit",
                    "useitem", "interactive", "protocol"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "pvp":
                    suggestions.addAll(Arrays.asList("spawn", "w-tap", "a-tap", "s-tap", "d-tap", "multipleweapons"));
                    break;
                case "ai":
                    suggestions.addAll(Arrays.asList("start", "stop", "status", "reload", "test", "act", "chat",
                            "forget", "defensive", "provider"));
                    break;
                case "control":
                case "remove":
                case "kit":
                    suggestions.addAll(getBotNames());
                    break;
                default:
                    break;
            }
        } else if (args.length == 3) {
            if ("kit".equalsIgnoreCase(args[0])) {
                suggestions.addAll(Arrays.asList("netherite_crystal", "diamond_crystal", "netherite_pot",
                        "diamond_pot", "netherite_basic", "diamond_basic"));
            }
        }
        return suggestions;
    }

    private boolean cmdRank(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§f/carpetplayers rank <set|list|remove|default>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "set": {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /carpetplayers rank set <player> <rank>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[2]);
                    return true;
                }
                Rank rank = Rank.fromName(args[3]);
                RankManager.setRank(target.getUniqueId(), rank);
                sender.sendMessage("§aSet rank of " + target.getName() + " to " + rank.getName());
                target.sendMessage("§aYour rank has been set to " + rank.getName());
                return true;
            }
            case "list": {
                Map<UUID, Rank> allRanks = RankManager.getAllRanks();
                if (allRanks.isEmpty()) {
                    sender.sendMessage("§eNo ranks assigned. Default: " + RankManager.getDefaultRank().getName());
                    return true;
                }
                sender.sendMessage("§6Ranks:");
                for (Map.Entry<UUID, Rank> entry : allRanks.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : entry.getKey().toString().substring(0, 8) + "...";
                    sender.sendMessage("§7  " + name + " §f-> §e" + entry.getValue().getName());
                }
                sender.sendMessage("§7Default: §f" + RankManager.getDefaultRank().getName());
                return true;
            }
            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers rank remove <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[2]);
                    return true;
                }
                RankManager.removeRank(target.getUniqueId());
                sender.sendMessage("§aRemoved rank for " + target.getName() + " (now: " + RankManager.getDefaultRank().getName() + ")");
                return true;
            }
            case "default": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /carpetplayers rank default <rank>");
                    return true;
                }
                Rank rank = Rank.fromName(args[2]);
                RankManager.setDefaultRank(rank);
                sender.sendMessage("§aDefault rank set to " + rank.getName());
                return true;
            }
            default:
                sender.sendMessage("§f/carpetplayers rank <set|list|remove|default>");
                return true;
        }
    }
}
