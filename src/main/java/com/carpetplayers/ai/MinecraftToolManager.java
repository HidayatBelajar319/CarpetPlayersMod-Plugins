package com.carpetplayers.ai;

import com.carpetplayers.bot.BotBrain;
import com.carpetplayers.bot.BotManager;
import com.carpetplayers.bot.KitManager;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinecraftToolManager {

    public static final MinecraftToolManager instance = new MinecraftToolManager();

    private final Map<String, AITool> tools = new LinkedHashMap<>();

    private static java.util.List<net.minecraft.world.level.block.state.BlockState> clipboard = null;
    private static int[] clipboardSize = null;

    private MinecraftToolManager() {
        registerDefaultTools();
    }

    private void registerDefaultTools() {
        tools.put("get_state", new AITool("get_state", "Get the bot's current state information", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    return bot.aiGetStateInfo();
                }));

        tools.put("move", new AITool("move", "Move the bot in a given direction for a number of ticks",
                AITool.objectParams(
                        AITool.enumParam("direction", "Movement direction: forward, back, left, right", true,
                                "forward", "back", "left", "right"),
                        AITool.intParam("ticks", "Movement duration in ticks (1-200)", false, 20, 1, 200)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String direction = args.has("direction") ? args.get("direction").getAsString() : "";
                    float forward = 0.0F;
                    float strafe = 0.0F;
                    if ("forward".equalsIgnoreCase(direction)) {
                        forward = 1.0F;
                    } else if ("back".equalsIgnoreCase(direction)) {
                        forward = -1.0F;
                    } else if ("left".equalsIgnoreCase(direction)) {
                        strafe = -1.0F;
                    } else if ("right".equalsIgnoreCase(direction)) {
                        strafe = 1.0F;
                    } else {
                        return "Invalid direction: " + direction;
                    }
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 20;
                    bot.aiMove(forward, strafe, ticks);
                    return "Bot moving " + direction + " for " + ticks + " tick(s)";
                }));

        tools.put("jump", new AITool("jump", "Make the bot jump for a number of ticks",
                AITool.objectParams(AITool.intParam("ticks", "Number of jump ticks (1-100)", false, 10, 1, 100)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 10;
                    bot.aiJump(ticks);
                    return "Bot jumping for " + ticks + " tick(s)";
                }));

        tools.put("sneak", new AITool("sneak", "Enable or disable bot sneaking for a number of ticks",
                AITool.objectParams(
                        AITool.booleanParam("enabled", "True to sneak, false to stop sneaking", true),
                        AITool.intParam("ticks", "Sneak duration in ticks (1-200)", false, 40, 1, 200)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    boolean enabled = args.has("enabled") && args.get("enabled").getAsBoolean();
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 40;
                    bot.aiSneak(enabled, ticks);
                    return "Bot " + (enabled ? "sneaking" : "stopped sneaking") + " for " + ticks + " tick(s)";
                }));

        tools.put("look_at", new AITool("look_at", "Point the bot's gaze at specific coordinates",
                AITool.objectParams(
                        AITool.doubleParam("x", "X coordinate", true),
                        AITool.doubleParam("y", "Y coordinate", true),
                        AITool.doubleParam("z", "Z coordinate", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    double x = args.get("x").getAsDouble();
                    double y = args.get("y").getAsDouble();
                    double z = args.get("z").getAsDouble();
                    bot.aiLookAt(x, y, z);
                    return "Bot looking at (" + x + "," + y + "," + z + ")";
                }));

        tools.put("attack", new AITool("attack", "Order the bot to attack the player named target",
                AITool.objectParams(AITool.stringParam("target", "Name of the player to attack", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String target = args.has("target") ? args.get("target").getAsString() : "";
                    if (target.isEmpty()) {
                        return "Empty target";
                    }
                    bot.aiAttack(target);
                    if (targetExists(target)) {
                        return "Attacking " + target;
                    }
                    return "Target " + target + " not found";
                }));

        tools.put("eat", new AITool("eat", "Order the bot to eat", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    bot.aiEat();
                    return "Bot eating";
                }));

        tools.put("chat", new AITool("chat", "Make the bot speak in chat",
                AITool.objectParams(AITool.stringParam("message", "Message for the bot to say", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String message = args.has("message") ? args.get("message").getAsString() : "";
                    bot.aiChat(message);
                    return "Bot said: " + message;
                }));

        tools.put("stop", new AITool("stop", "Stop all bot actions", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    bot.aiStop();
                    return "Bot stopped";
                }));

        tools.put("set_state", new AITool("set_state", "Change the bot's state",
                AITool.objectParams(AITool.enumParam("state", "New state: follow, wander, pvp, chill, eat", true,
                        "follow", "wander", "pvp", "chill", "eat")),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String stateName = args.has("state") ? args.get("state").getAsString() : "";
                    if (stateName.isEmpty()) {
                        return "Empty state";
                    }
                    try {
                        BotBrain.BotState state = BotBrain.BotState.valueOf(stateName.toUpperCase());
                        bot.aiSetState(state);
                        return "Bot state -> " + state.name();
                    } catch (IllegalArgumentException e) {
                        return "Invalid state: " + stateName;
                    }
                }));

        tools.put("mine_block", new AITool("mine_block",
                "Order the bot to mine a block at specific coordinates (or the nearest one)",
                AITool.objectParams(
                        AITool.doubleParam("x", "X coordinate", false),
                        AITool.doubleParam("y", "Y coordinate", false),
                        AITool.doubleParam("z", "Z coordinate", false)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    if (args.has("x") && args.has("y") && args.has("z")) {
                        int x = args.get("x").getAsInt();
                        int y = args.get("y").getAsInt();
                        int z = args.get("z").getAsInt();
                        bot.aiMineAt(x, y, z);
                        return "Bot mining block at (" + x + "," + y + "," + z + ")";
                    }
                    bot.aiMineNearest();
                    return "Bot mining nearest block";
                }));

        tools.put("use_item", new AITool("use_item", "Use the item in the bot's hand",
                AITool.objectParams(AITool.intParam("slot", "Item slot (0-8)", false, -1, -1, 8)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    int slot = args.has("slot") ? args.get("slot").getAsInt() : -1;
                    if (slot >= 0) {
                        bot.aiSelectSlot(slot);
                    }
                    bot.aiUseItem();
                    return "Bot using item";
                }));

        tools.put("drop_item", new AITool("drop_item", "Bot drops the item from its hand",
                AITool.objectParams(AITool.booleanParam("all", "True to drop all items", false)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    boolean all = args.has("all") && args.get("all").getAsBoolean();
                    bot.aiDropItem(all);
                    return all ? "Bot dropping all items" : "Bot dropping item";
                }));

        tools.put("equip_kit", new AITool("equip_kit", "Equip a PvP kit on the bot",
                AITool.objectParams(AITool.enumParam("kit", "PvP kit: netherite_crystal, diamond_crystal, netherite_pot, diamond_pot, netherite_basic, diamond_basic", true,
                        "netherite_crystal", "diamond_crystal", "netherite_pot", "diamond_pot", "netherite_basic", "diamond_basic")),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String kit = args.has("kit") ? args.get("kit").getAsString() : "";
                    boolean ok = KitManager.applyKit(bot, kit);
                    return ok ? "Kit " + kit + " equipped" : "Unknown kit: " + kit;
                }));

        tools.put("run_command", new AITool("run_command",
                "Execute a server command as the bot (e.g. /give, /tp, /effect, /gamemode). The command runs with the bot as the executor so permissions and position are correct.",
                AITool.objectParams(
                        AITool.stringParam("command", "The server command to execute (include the / prefix)", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String command = args.has("command") ? args.get("command").getAsString() : "";
                    if (command.isEmpty()) {
                        return "Empty command";
                    }
                    return bot.aiRunCommand(command);
                }));

        tools.put("set_blocks", new AITool("set_blocks",
                "Set all blocks in a rectangular region to a specified block type. Like WorldEdit //set command.",
                AITool.objectParams(
                        AITool.intParam("x1", "Start X coordinate", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Start Y coordinate", true, 0, 0, 255),
                        AITool.intParam("z1", "Start Z coordinate", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "End X coordinate", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "End Y coordinate", true, 0, 0, 255),
                        AITool.intParam("z2", "End Z coordinate", true, 0, -30000000, 30000000),
                        AITool.stringParam("block", "Block type (e.g. 'minecraft:stone', 'minecraft:dirt')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    net.minecraft.world.level.block.state.BlockState state;
                    try {
                        state = blockStateFromString(args.get("block").getAsString());
                    } catch (IllegalArgumentException e) {
                        return "Invalid block: " + args.get("block").getAsString();
                    }
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.world.level.Level level = bot.getBot().getLevel();
                    int count = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, y, z), state);
                                count++;
                            }
                        }
                    }
                    return "Set " + count + " blocks to " + args.get("block").getAsString();
                }));

        tools.put("replace_blocks", new AITool("replace_blocks",
                "Replace all blocks of one type with another in a rectangular region. Like WorldEdit //replace.",
                AITool.objectParams(
                        AITool.intParam("x1", "Start X", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Start Y", true, 0, 0, 255),
                        AITool.intParam("z1", "Start Z", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "End X", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "End Y", true, 0, 0, 255),
                        AITool.intParam("z2", "End Z", true, 0, -30000000, 30000000),
                        AITool.stringParam("from", "Source block type to replace (e.g. 'minecraft:stone')", true),
                        AITool.stringParam("to", "Target block type (e.g. 'minecraft:dirt')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    net.minecraft.world.level.block.state.BlockState fromState;
                    net.minecraft.world.level.block.state.BlockState toState;
                    try {
                        fromState = blockStateFromString(args.get("from").getAsString());
                        toState = blockStateFromString(args.get("to").getAsString());
                    } catch (IllegalArgumentException e) {
                        return "Invalid block type: " + e.getMessage();
                    }
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.world.level.Level level = bot.getBot().getLevel();
                    int count = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                                if (level.getBlockState(pos).equals(fromState)) {
                                    level.setBlockAndUpdate(pos, toState);
                                    count++;
                                }
                            }
                        }
                    }
                    return "Replaced " + count + " " + args.get("from").getAsString() + " with " + args.get("to").getAsString();
                }));

        tools.put("read_file", new AITool("read_file",
                "Read the contents of a file from the mod's source directory. Useful for analyzing code. Only reads .java and .json files.",
                AITool.objectParams(
                        AITool.stringParam("path", "Relative file path from the project root (e.g. 'src/main/java/com/carpetplayers/bot/BotBrain.java')", true)),
                (args, bot) -> {
                    String path = args.has("path") ? args.get("path").getAsString() : "";
                    if (path.isEmpty()) return "Empty path";
                    if (!path.endsWith(".java") && !path.endsWith(".json") && !path.endsWith(".yml")) {
                        return "Only .java, .json, and .yml files are allowed for security";
                    }
                    try {
                        java.nio.file.Path filePath = net.fabricmc.loader.api.FabricLoader.getInstance()
                                .getGameDirectory().toPath().resolve(path).normalize();
                        if (!filePath.startsWith(net.fabricmc.loader.api.FabricLoader.getInstance()
                                .getGameDirectory().toPath())) {
                            return "Path traversal not allowed";
                        }
                        if (!java.nio.file.Files.exists(filePath)) {
                            return "File not found: " + path;
                        }
                        if (java.nio.file.Files.size(filePath) > 64 * 1024) {
                            return "File too large (>64KB). Limit: 64KB";
                        }
                        String content = new String(java.nio.file.Files.readAllBytes(filePath));
                        if (content.length() > 4000) {
                            content = content.substring(0, 4000) + "\n... (truncated at 4000 chars)";
                        }
                        return "File: " + path + "\n---\n" + content;
                    } catch (Exception e) {
                        return "Error reading file: " + e.getMessage();
                    }
                }));

        tools.put("group_command", new AITool("group_command",
                "Execute a server command for multiple bots at once. Provide bot names as comma-separated list.",
                AITool.objectParams(
                        AITool.stringParam("bots", "Comma-separated bot names (e.g. 'FriendBot_1,FriendBot_2')", true),
                        AITool.stringParam("command", "Server command to execute for each bot (e.g. '/effect give @s speed 30')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    String botNames = args.has("bots") ? args.get("bots").getAsString() : "";
                    String command = args.has("command") ? args.get("command").getAsString() : "";
                    if (botNames.isEmpty() || command.isEmpty()) return "Both 'bots' and 'command' are required";
                    String[] names = botNames.split(",");
                    int success = 0, failed = 0;
                    for (String name : names) {
                        String trimmed = name.trim();
                        BotBrain target = findBotByName(trimmed);
                        if (target == null) {
                            failed++;
                            continue;
                        }
                        String result = target.aiRunCommand(command);
                        if (result.startsWith("Command executed")) {
                            success++;
                        } else {
                            failed++;
                        }
                    }
                    return "Group command: " + success + " succeeded, " + failed + " failed out of " + names.length;
                }));

        tools.put("copy_region", new AITool("copy_region",
                "Copy all blocks in a rectangular region to an in-memory clipboard. Use paste_region to paste.",
                AITool.objectParams(
                        AITool.intParam("x1", "Start X", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Start Y", true, 0, 0, 255),
                        AITool.intParam("z1", "Start Z", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "End X", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "End Y", true, 0, 0, 255),
                        AITool.intParam("z2", "End Z", true, 0, -30000000, 30000000)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.world.level.Level level = bot.getBot().getLevel();
                    java.util.List<net.minecraft.world.level.block.state.BlockState> blocks = new java.util.ArrayList<>();
                    int w = maxX - minX + 1, h = maxY - minY + 1, d = maxZ - minZ + 1;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                blocks.add(level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)));
                            }
                        }
                    }
                    clipboard = blocks;
                    clipboardSize = new int[]{w, h, d};
                    return "Copied " + blocks.size() + " blocks (" + w + "x" + h + "x" + d + ") to clipboard";
                }));

        tools.put("paste_region", new AITool("paste_region",
                "Paste a previously copied region to a target location.",
                AITool.objectParams(
                        AITool.intParam("x", "Target X coordinate", true, 0, -30000000, 30000000),
                        AITool.intParam("y", "Target Y coordinate", true, 0, 0, 255),
                        AITool.intParam("z", "Target Z coordinate", true, 0, -30000000, 30000000)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    if (clipboard == null || clipboard.isEmpty()) return "No region in clipboard. Use copy_region first.";
                    int tx = args.get("x").getAsInt(), ty = args.get("y").getAsInt(), tz = args.get("z").getAsInt();
                    net.minecraft.world.level.Level level = bot.getBot().getLevel();
                    int idx = 0;
                    for (int x = 0; x < clipboardSize[0]; x++) {
                        for (int y = 0; y < clipboardSize[1]; y++) {
                            for (int z = 0; z < clipboardSize[2]; z++) {
                                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(tx + x, ty + y, tz + z), clipboard.get(idx));
                                idx++;
                            }
                        }
                    }
                    return "Pasted " + clipboard.size() + " blocks at (" + tx + "," + ty + "," + tz + ")";
                }));

        tools.put("navigate_to", new AITool("navigate_to", "Navigate the bot to a specific position using A* pathfinding",
                AITool.objectParams(
                        AITool.intParam("x", "Target X coordinate", true, 0, -30000000, 30000000),
                        AITool.intParam("y", "Target Y coordinate", true, 0, 0, 255),
                        AITool.intParam("z", "Target Z coordinate", true, 0, -30000000, 30000000)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    int x = args.get("x").getAsInt();
                    int y = args.get("y").getAsInt();
                    int z = args.get("z").getAsInt();
                    bot.navigateTo(new net.minecraft.core.BlockPos(x, y, z));
                    return "Navigating to (" + x + ", " + y + ", " + z + ")";
                }));
    }

    /**
     * Resolves a block name (e.g. "minecraft:stone") to its default BlockState.
     * Throws IllegalArgumentException for unknown or malformed names.
     */
    private static net.minecraft.world.level.block.state.BlockState blockStateFromString(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("empty block name");
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);
        if (id == null || !net.minecraft.core.Registry.BLOCK.containsKey(id)) {
            throw new IllegalArgumentException("unknown block: " + name);
        }
        return net.minecraft.core.Registry.BLOCK.get(id).defaultBlockState();
    }

    private static boolean targetExists(String name) {
        if (MinecraftToolManager.findBotByName(name) != null) {
            return true;
        }
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (brain.getBot().getServer() != null
                    && brain.getBot().getServer().getPlayerList().getPlayerByName(name) != null) {
                return true;
            }
        }
        return false;
    }

    private static String noBot() {
        return "Bot not found (may have been removed)";
    }

    public List<AITool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    /**
     * Executes a tool by name. Never throws exceptions;
     * all failures are returned as error message strings.
     */
    public String executeTool(String toolName, JsonObject args, BotBrain bot) {
        AITool tool = tools.get(toolName);
        if (tool == null) {
            return "Unknown tool: " + toolName;
        }
        try {
            if (args == null) {
                args = new JsonObject();
            }
            return tool.execute(args, bot);
        } catch (Exception e) {
            return "Error executing tool " + toolName + ": "
                    + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    public static BotBrain findBotByName(String name) {
        if (name == null) {
            return null;
        }
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (brain.getBotName().equalsIgnoreCase(name)
                    || brain.getBot().getName().getString().equalsIgnoreCase(name)) {
                return brain;
            }
        }
        return null;
    }
}
