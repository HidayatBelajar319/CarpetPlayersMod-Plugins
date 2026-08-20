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

    private static java.util.List<net.minecraft.server.v1_16_R3.IBlockData> clipboard = null;
    private static int[] clipboardSize = null;

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

        tools.put("run_command", new AITool("run_command",
                "Jalankan perintah server sebagai bot.",
                AITool.objectParams(
                        AITool.stringParam("command", "Perintah server yang akan dijalankan (dengan prefix /)", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    String command = args.has("command") ? args.get("command").getAsString() : "";
                    if (command.isEmpty()) return "Perintah kosong";
                    return bot.aiRunCommand(command);
                }));

        tools.put("set_blocks", new AITool("set_blocks",
                "Set semua blok di area persegi panjang menjadi tipe blok tertentu. Seperti perintah WorldEdit //set.",
                AITool.objectParams(
                        AITool.intParam("x1", "Koordinat X awal", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Koordinat Y awal", true, 0, 0, 255),
                        AITool.intParam("z1", "Koordinat Z awal", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "Koordinat X akhir", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "Koordinat Y akhir", true, 0, 0, 255),
                        AITool.intParam("z2", "Koordinat Z akhir", true, 0, -30000000, 30000000),
                        AITool.stringParam("block", "Tipe blok (mis. 'minecraft:stone', 'minecraft:dirt')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    net.minecraft.server.v1_16_R3.IBlockData state;
                    try {
                        state = blockStateFromString(args.get("block").getAsString());
                    } catch (IllegalArgumentException e) {
                        return "Blok tidak valid: " + args.get("block").getAsString();
                    }
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.server.v1_16_R3.World level = bot.getBot().getWorld();
                    int count = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                level.setTypeUpdate(new net.minecraft.server.v1_16_R3.BlockPosition(x, y, z), state);
                                count++;
                            }
                        }
                    }
                    return "Set " + count + " blok menjadi " + args.get("block").getAsString();
                }));

        tools.put("replace_blocks", new AITool("replace_blocks",
                "Ganti semua blok dari satu tipe ke tipe lain di area persegi panjang. Seperti WorldEdit //replace.",
                AITool.objectParams(
                        AITool.intParam("x1", "Koordinat X awal", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Koordinat Y awal", true, 0, 0, 255),
                        AITool.intParam("z1", "Koordinat Z awal", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "Koordinat X akhir", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "Koordinat Y akhir", true, 0, 0, 255),
                        AITool.intParam("z2", "Koordinat Z akhir", true, 0, -30000000, 30000000),
                        AITool.stringParam("from", "Tipe blok sumber yang diganti (mis. 'minecraft:stone')", true),
                        AITool.stringParam("to", "Tipe blok target (mis. 'minecraft:dirt')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    net.minecraft.server.v1_16_R3.IBlockData fromState;
                    net.minecraft.server.v1_16_R3.IBlockData toState;
                    try {
                        fromState = blockStateFromString(args.get("from").getAsString());
                        toState = blockStateFromString(args.get("to").getAsString());
                    } catch (IllegalArgumentException e) {
                        return "Tipe blok tidak valid: " + e.getMessage();
                    }
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.server.v1_16_R3.World level = bot.getBot().getWorld();
                    int count = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                net.minecraft.server.v1_16_R3.BlockPosition pos = new net.minecraft.server.v1_16_R3.BlockPosition(x, y, z);
                                if (level.getType(pos).equals(fromState)) {
                                    level.setTypeUpdate(pos, toState);
                                    count++;
                                }
                            }
                        }
                    }
                    return "Mengganti " + count + " " + args.get("from").getAsString() + " dengan " + args.get("to").getAsString();
                }));

        tools.put("read_file", new AITool("read_file",
                "Baca isi file dari direktori root server. Berguna untuk menganalisis kode. Hanya membaca file .java, .json, dan .yml.",
                AITool.objectParams(
                        AITool.stringParam("path", "Path file relatif dari root server (mis. 'plugins/MyPlugin/src/main/java/com/carpetplayers/bot/BotBrain.java')", true)),
                (args, bot) -> {
                    String path = args.has("path") ? args.get("path").getAsString() : "";
                    if (path.isEmpty()) return "Path kosong";
                    if (!path.endsWith(".java") && !path.endsWith(".json") && !path.endsWith(".yml")) {
                        return "Hanya file .java, .json, dan .yml yang diizinkan demi keamanan";
                    }
                    try {
                        java.nio.file.Path basePath = org.bukkit.Bukkit.getServer().getWorldContainer().toPath();
                        java.nio.file.Path filePath = basePath.resolve(path).normalize();
                        if (!filePath.startsWith(basePath)) {
                            return "Path traversal tidak diizinkan";
                        }
                        if (!java.nio.file.Files.exists(filePath)) {
                            return "File tidak ditemukan: " + path;
                        }
                        if (java.nio.file.Files.size(filePath) > 64 * 1024) {
                            return "File terlalu besar (>64KB). Batas: 64KB";
                        }
                        String content = new String(java.nio.file.Files.readAllBytes(filePath));
                        if (content.length() > 4000) {
                            content = content.substring(0, 4000) + "\n... (dipotong pada 4000 karakter)";
                        }
                        return "File: " + path + "\n---\n" + content;
                    } catch (Exception e) {
                        return "Error membaca file: " + e.getMessage();
                    }
                }));

        tools.put("group_command", new AITool("group_command",
                "Jalankan perintah server untuk beberapa bot sekaligus. Berikan nama bot sebagai daftar dipisah koma.",
                AITool.objectParams(
                        AITool.stringParam("bots", "Nama bot dipisah koma (mis. 'FriendBot_1,FriendBot_2')", true),
                        AITool.stringParam("command", "Perintah server untuk dijalankan untuk setiap bot (mis. '/effect give @s speed 30')", true)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    String botNames = args.has("bots") ? args.get("bots").getAsString() : "";
                    String command = args.has("command") ? args.get("command").getAsString() : "";
                    if (botNames.isEmpty() || command.isEmpty()) return "'bots' dan 'command' wajib diisi";
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
                    return "Perintah grup: " + success + " berhasil, " + failed + " gagal dari " + names.length;
                }));

        tools.put("copy_region", new AITool("copy_region",
                "Salin semua blok di area persegi panjang ke clipboard memori. Gunakan paste_region untuk menempel.",
                AITool.objectParams(
                        AITool.intParam("x1", "Koordinat X awal", true, 0, -30000000, 30000000),
                        AITool.intParam("y1", "Koordinat Y awal", true, 0, 0, 255),
                        AITool.intParam("z1", "Koordinat Z awal", true, 0, -30000000, 30000000),
                        AITool.intParam("x2", "Koordinat X akhir", true, 0, -30000000, 30000000),
                        AITool.intParam("y2", "Koordinat Y akhir", true, 0, 0, 255),
                        AITool.intParam("z2", "Koordinat Z akhir", true, 0, -30000000, 30000000)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
                    int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
                    int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
                    int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
                    int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
                    net.minecraft.server.v1_16_R3.World level = bot.getBot().getWorld();
                    java.util.List<net.minecraft.server.v1_16_R3.IBlockData> blocks = new java.util.ArrayList<>();
                    int w = maxX - minX + 1, h = maxY - minY + 1, d = maxZ - minZ + 1;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                blocks.add(level.getType(new net.minecraft.server.v1_16_R3.BlockPosition(x, y, z)));
                            }
                        }
                    }
                    clipboard = blocks;
                    clipboardSize = new int[]{w, h, d};
                    return "Menyalin " + blocks.size() + " blok (" + w + "x" + h + "x" + d + ") ke clipboard";
                }));

        tools.put("paste_region", new AITool("paste_region",
                "Tempel area yang sebelumnya disalin ke lokasi target.",
                AITool.objectParams(
                        AITool.intParam("x", "Koordinat X target", true, 0, -30000000, 30000000),
                        AITool.intParam("y", "Koordinat Y target", true, 0, 0, 255),
                        AITool.intParam("z", "Koordinat Z target", true, 0, -30000000, 30000000)),
                (args, bot) -> {
                    if (bot == null) return noBot();
                    if (clipboard == null || clipboard.isEmpty()) return "Tidak ada area di clipboard. Gunakan copy_region dulu.";
                    int tx = args.get("x").getAsInt(), ty = args.get("y").getAsInt(), tz = args.get("z").getAsInt();
                    net.minecraft.server.v1_16_R3.World level = bot.getBot().getWorld();
                    int idx = 0;
                    for (int x = 0; x < clipboardSize[0]; x++) {
                        for (int y = 0; y < clipboardSize[1]; y++) {
                            for (int z = 0; z < clipboardSize[2]; z++) {
                                level.setTypeUpdate(new net.minecraft.server.v1_16_R3.BlockPosition(tx + x, ty + y, tz + z), clipboard.get(idx));
                                idx++;
                            }
                        }
                    }
                    return "Menempel " + clipboard.size() + " blok di (" + tx + "," + ty + "," + tz + ")";
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

    /**
     * Menyelesaikan nama blok (mis. "minecraft:stone") menjadi IBlockData default.
     * Melempar IllegalArgumentException untuk nama yang tidak dikenal atau salah format.
     */
    private static net.minecraft.server.v1_16_R3.IBlockData blockStateFromString(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("empty block name");
        net.minecraft.server.v1_16_R3.MinecraftKey id;
        try {
            id = net.minecraft.server.v1_16_R3.MinecraftKey.a(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid block name: " + name);
        }
        net.minecraft.server.v1_16_R3.Block block = net.minecraft.server.v1_16_R3.IRegistry.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("unknown block: " + name));
        return block.getBlockData();
    }
}