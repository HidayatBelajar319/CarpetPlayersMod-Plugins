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
                    return "Bot is moving " + direction + " for " + ticks + " ticks";
                }));

        tools.put("jump", new AITool("jump", "Make the bot jump for a number of ticks",
                AITool.objectParams(AITool.intParam("ticks", "Number of jumping ticks (1-100)", false, 10, 1, 100)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 10;
                    bot.aiJump(ticks);
                    return "Bot jumped for " + ticks + " ticks";
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
                    return "Bot is " + (enabled ? "sneaking" : "not sneaking") + " for " + ticks + " ticks";
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
                    return "Bot is looking at (" + x + "," + y + "," + z + ")";
                }));

        tools.put("attack", new AITool("attack", "Command the bot to attack the player named target",
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

        tools.put("eat", new AITool("eat", "Command the bot to eat", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    bot.aiEat();
                    return "Bot is eating";
                }));

        tools.put("chat", new AITool("chat", "Make the bot speak in chat",
                AITool.objectParams(AITool.stringParam("message", "Message the bot says", true)),
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
                "Command the bot to mine a block at specific coordinates (or the nearest one)",
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
                        return "Bot is mining the block at (" + x + "," + y + "," + z + ")";
                    }
                    bot.aiMineNearest();
                    return "Bot is mining the nearest block";
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
                    return "Bot is using the item";
                }));

        tools.put("drop_item", new AITool("drop_item", "Bot drops the item from its hand",
                AITool.objectParams(AITool.booleanParam("all", "True to drop all items", false)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    boolean all = args.has("all") && args.get("all").getAsBoolean();
                    bot.aiDropItem(all);
                    return all ? "Bot dropped all items" : "Bot dropped the item";
                }));

        tools.put("equip_kit", new AITool("equip_kit", "Equip a PvP kit to the bot",
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
    }

    private static boolean targetExists(String name) {
        if (MinecraftToolManager.findBotByName(name) != null) {
            return true;
        }
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (brain.getBot().getServer() != null
                    && brain.getBot().getServer().getPlayerList().getPlayer(name) != null) {
                return true;
            }
        }
        return false;
    }

    private static String noBot() {
        return "Bot not found (it may have been removed)";
    }

    public List<AITool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    /**
     * Executes a tool by name. Never throws an exception;
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
