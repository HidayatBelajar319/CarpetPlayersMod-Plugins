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
        tools.put("get_state", new AITool("get_state", "Ambil informasi state bot saat ini", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    return bot.aiGetStateInfo();
                }));

        tools.put("move", new AITool("move", "Gerakkan bot ke arah tertentu selama beberapa tick",
                AITool.objectParams(
                        AITool.enumParam("direction", "Arah gerakan: forward, back, left, right", true,
                                "forward", "back", "left", "right"),
                        AITool.intParam("ticks", "Durasi gerakan dalam tick (1-200)", false, 20, 1, 200)),
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
                        return "Arah tidak valid: " + direction;
                    }
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 20;
                    bot.aiMove(forward, strafe, ticks);
                    return "Bot bergerak " + direction + " selama " + ticks + " tick";
                }));

        tools.put("jump", new AITool("jump", "Buat bot melompat selama beberapa tick",
                AITool.objectParams(AITool.intParam("ticks", "Jumlah tick melompat (1-100)", false, 10, 1, 100)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 10;
                    bot.aiJump(ticks);
                    return "Bot melompat selama " + ticks + " tick";
                }));

        tools.put("sneak", new AITool("sneak", "Aktifkan atau nonaktifkan sneak bot selama beberapa tick",
                AITool.objectParams(
                        AITool.booleanParam("enabled", "True untuk sneak, false untuk berhenti sneak", true),
                        AITool.intParam("ticks", "Durasi sneak dalam tick (1-200)", false, 40, 1, 200)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    boolean enabled = args.has("enabled") && args.get("enabled").getAsBoolean();
                    int ticks = args.has("ticks") ? args.get("ticks").getAsInt() : 40;
                    bot.aiSneak(enabled, ticks);
                    return "Bot " + (enabled ? "sneak" : "berhenti sneak") + " selama " + ticks + " tick";
                }));

        tools.put("look_at", new AITool("look_at", "Arahkan pandangan bot ke koordinat tertentu",
                AITool.objectParams(
                        AITool.doubleParam("x", "Koordinat X", true),
                        AITool.doubleParam("y", "Koordinat Y", true),
                        AITool.doubleParam("z", "Koordinat Z", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    double x = args.get("x").getAsDouble();
                    double y = args.get("y").getAsDouble();
                    double z = args.get("z").getAsDouble();
                    bot.aiLookAt(x, y, z);
                    return "Bot melihat ke (" + x + "," + y + "," + z + ")";
                }));

        tools.put("attack", new AITool("attack", "Perintahkan bot menyerang pemain bernama target",
                AITool.objectParams(AITool.stringParam("target", "Nama pemain yang menjadi target serangan", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String target = args.has("target") ? args.get("target").getAsString() : "";
                    if (target.isEmpty()) {
                        return "Target kosong";
                    }
                    bot.aiAttack(target);
                    if (targetExists(target)) {
                        return "Menyerang " + target;
                    }
                    return "Target " + target + " tidak ditemukan";
                }));

        tools.put("eat", new AITool("eat", "Perintahkan bot makan", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    bot.aiEat();
                    return "Bot makan";
                }));

        tools.put("chat", new AITool("chat", "Buat bot berkata di chat",
                AITool.objectParams(AITool.stringParam("message", "Pesan yang diucapkan bot", true)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String message = args.has("message") ? args.get("message").getAsString() : "";
                    bot.aiChat(message);
                    return "Bot berkata: " + message;
                }));

        tools.put("stop", new AITool("stop", "Hentikan semua aksi bot", AITool.noParams(),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    bot.aiStop();
                    return "Bot berhenti";
                }));

        tools.put("set_state", new AITool("set_state", "Ubah state bot",
                AITool.objectParams(AITool.enumParam("state", "State baru: follow, wander, pvp, chill, eat", true,
                        "follow", "wander", "pvp", "chill", "eat")),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String stateName = args.has("state") ? args.get("state").getAsString() : "";
                    if (stateName.isEmpty()) {
                        return "State kosong";
                    }
                    try {
                        BotBrain.BotState state = BotBrain.BotState.valueOf(stateName.toUpperCase());
                        bot.aiSetState(state);
                        return "State bot -> " + state.name();
                    } catch (IllegalArgumentException e) {
                        return "State tidak valid: " + stateName;
                    }
                }));

        tools.put("mine_block", new AITool("mine_block",
                "Perintahkan bot menambang blok di koordinat tertentu (atau terdekat)",
                AITool.objectParams(
                        AITool.doubleParam("x", "Koordinat X", false),
                        AITool.doubleParam("y", "Koordinat Y", false),
                        AITool.doubleParam("z", "Koordinat Z", false)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    if (args.has("x") && args.has("y") && args.has("z")) {
                        int x = args.get("x").getAsInt();
                        int y = args.get("y").getAsInt();
                        int z = args.get("z").getAsInt();
                        bot.aiMineAt(x, y, z);
                        return "Bot menambang blok di (" + x + "," + y + "," + z + ")";
                    }
                    bot.aiMineNearest();
                    return "Bot menambang blok terdekat";
                }));

        tools.put("use_item", new AITool("use_item", "Gunakan item di tangan bot",
                AITool.objectParams(AITool.intParam("slot", "Slot item (0-8)", false, -1, -1, 8)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    int slot = args.has("slot") ? args.get("slot").getAsInt() : -1;
                    if (slot >= 0) {
                        bot.aiSelectSlot(slot);
                    }
                    bot.aiUseItem();
                    return "Bot menggunakan item";
                }));

        tools.put("drop_item", new AITool("drop_item", "Bot membuang item dari tangan",
                AITool.objectParams(AITool.booleanParam("all", "True untuk membuang semua item", false)),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    boolean all = args.has("all") && args.get("all").getAsBoolean();
                    bot.aiDropItem(all);
                    return all ? "Bot membuang item (semua)" : "Bot membuang item";
                }));

        tools.put("equip_kit", new AITool("equip_kit", "Pasang kit PvP ke bot",
                AITool.objectParams(AITool.enumParam("kit", "Kit PvP: netherite_crystal, diamond_crystal, netherite_pot, diamond_pot, netherite_basic, diamond_basic", true,
                        "netherite_crystal", "diamond_crystal", "netherite_pot", "diamond_pot", "netherite_basic", "diamond_basic")),
                (args, bot) -> {
                    if (bot == null) {
                        return noBot();
                    }
                    String kit = args.has("kit") ? args.get("kit").getAsString() : "";
                    boolean ok = KitManager.applyKit(bot, kit);
                    return ok ? "Kit " + kit + " dipasang" : "Kit tidak dikenal: " + kit;
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
        return "Bot tidak ditemukan (mungkin sudah dihapus)";
    }

    public List<AITool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    /**
     * Menjalankan tool berdasarkan nama. Tidak pernah melempar exception;
     * semua kegagalan dikembalikan sebagai string pesan error.
     */
    public String executeTool(String toolName, JsonObject args, BotBrain bot) {
        AITool tool = tools.get(toolName);
        if (tool == null) {
            return "Tool tidak dikenal: " + toolName;
        }
        try {
            if (args == null) {
                args = new JsonObject();
            }
            return tool.execute(args, bot);
        } catch (Exception e) {
            return "Error mengeksekusi tool " + toolName + ": "
                    + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    public static BotBrain findBotByName(String name) {
        if (name == null) {
            return null;
        }
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (brain.getBotName().equalsIgnoreCase(name)
                    || brain.getBot().getName().equalsIgnoreCase(name)) {
                return brain;
            }
        }
        return null;
    }
}