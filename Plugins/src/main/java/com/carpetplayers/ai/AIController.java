package com.carpetplayers.ai;

import com.carpetplayers.CarpetPlayersPlugin;
import com.carpetplayers.bot.BotBrain;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AIController {

    private static final int MAX_ITERATIONS = 6;
    private static final int TOOL_TIMEOUT_SECONDS = 5;
    private static final int MAX_MESSAGES = 24;
    private static final int MAX_TOOL_RESULT_CHARS = 800;
    private static final int MEMORY_MAX = 12;
    private static final int MAX_REPLY_CHARS = 500;
    private static final Map<UUID, List<AIMessage>> chatMemories = new ConcurrentHashMap<>();
    private static final Map<UUID, List<AIMessage>> actMemories = new ConcurrentHashMap<>();

    private AIController() {
    }

    /**
     * Menjalankan loop tool-calling AI untuk sebuah bot.
     * Seluruh proses berjalan di thread daemon terpisah sehingga tidak pernah
     * memblokir server thread. Semua mutasi bot dan semua callback dipanggil
     * di server thread.
     */
    public static void run(String botName, String instruction, Consumer<String> onResult, Consumer<String> onError) {
        Thread controlThread = new Thread(
                () -> runOnControlThread(botName, instruction, onResult, onError),
                "carpetplayers-ai-control");
        controlThread.setDaemon(true);
        controlThread.start();
    }

    /**
     * Menjalankan percakapan AI untuk sebuah bot (dengan memori per-bot).
     * Berjalan di thread daemon terpisah; semua mutasi bot dan broadcast
     * dipanggil di server thread.
     */
    public static void runChat(String botName, String instruction) {
        Thread controlThread = new Thread(
                () -> runChatOnControlThread(botName, instruction),
                "carpetplayers-ai-chat");
        controlThread.setDaemon(true);
        controlThread.start();
    }

    /**
     * Membersihkan memori percakapan sebuah bot. No-op jika bot tidak ada.
     * Aman dipanggil dari server thread (menggunakan ConcurrentHashMap).
     */
    public static void clearMemory(String botName) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        if (bot != null) {
            chatMemories.remove(bot.getUuid());
            actMemories.remove(bot.getUuid());
        }
    }

    private static void runOnControlThread(String botName, String instruction,
                                           Consumer<String> onResult, Consumer<String> onError) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        MinecraftServer server = bot != null ? bot.getBot().getServer() : null;

        if (server != null) {
            AIProviderManager.instance().ensureLoaded();
        }

        if (bot == null) {
            deliverResult(server, onError, "Bot " + botName + " tidak ditemukan");
            return;
        }
        UUID key = bot.getUuid();
        List<AIMessage> memory = actMemories.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<AIMessage>()));
        List<AIMessage> messages = new ArrayList<>(memory);
        if (memory.isEmpty()) {
            messages.add(AIMessage.user("Bot: " + bot.aiGetStateInfo() + "\nInstruksi pemain: " + instruction));
        } else {
            messages.add(AIMessage.user(instruction));
        }

        String lastContent = "";
        String finalReply = "";
        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                while (messages.size() > MAX_MESSAGES) {
                    messages.remove(1);
                }
                AIResponse response = AIProviderManager.instance().sendMessageWithTools(
                        messages, MinecraftToolManager.instance.getTools());
                if (response == null) {
                    deliverResult(server, onError, "Respon AI kosong");
                    return;
                }
                lastContent = response.content != null ? response.content : "";
                if (response.toolCalls == null || response.toolCalls.isEmpty()) {
                    finalReply = lastContent;
                    break;
                }
                messages.add(AIMessage.assistantWithTools(lastContent, response.toolCalls));
                for (AIToolCall toolCall : response.toolCalls) {
                    String result = executeToolOnServer(server, bot, toolCall);
                    messages.add(AIMessage.tool(toolCall.id, result));
                }
            }
            if (finalReply.isEmpty()) {
                finalReply = lastContent.isEmpty()
                        ? "Selesai (maks iterasi tool call)" : lastContent;
            }
            synchronized (memory) {
                memory.add(AIMessage.user(instruction));
                memory.add(AIMessage.assistant(finalReply));
                while (memory.size() > MEMORY_MAX) {
                    memory.remove(0);
                }
            }
            deliverResult(server, onResult, finalReply);
        } catch (AIException e) {
            deliverResult(server, onError,
                    e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static void runChatOnControlThread(String botName, String instruction) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        if (bot == null) {
            return;
        }
        MinecraftServer server = bot.getBot().getServer();
        if (server != null) {
            AIProviderManager.instance().ensureLoaded();
        }

        if (!AIProviderManager.instance().isEnabled()) {
            deliverResult(server, reply -> broadcastChatReply(botName, "AI nonaktif. Ketik /carpetplayers ai start"), null);
            return;
        }

        UUID key = bot.getUuid();
        List<AIMessage> memory = chatMemories.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<AIMessage>()));
        List<AIMessage> messages = new ArrayList<>(memory);
        messages.add(AIMessage.user(instruction));

        String reply = "";
        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                while (messages.size() > MAX_MESSAGES) {
                    messages.remove(1);
                }
                AIResponse response = AIProviderManager.instance().sendMessageWithTools(
                        messages, MinecraftToolManager.instance.getTools());
                if (response == null) {
                    deliverResult(server, r -> broadcastChatReply(botName, "Respon AI kosong"), null);
                    return;
                }
                reply = response.content != null ? response.content : "";
                if (response.toolCalls == null || response.toolCalls.isEmpty()) {
                    break;
                }
                messages.add(AIMessage.assistantWithTools(reply, response.toolCalls));
                for (AIToolCall toolCall : response.toolCalls) {
                    String result = executeToolOnServer(server, bot, toolCall);
                    messages.add(AIMessage.tool(toolCall.id, result));
                }
            }
            String finalReply = truncateReply(reply.isEmpty()
                    ? "Selesai (maks iterasi tool call)" : reply);
            deliverResult(server, r -> broadcastChatReply(botName, r), finalReply);
            synchronized (memory) {
                memory.add(AIMessage.user(instruction));
                memory.add(AIMessage.assistant(finalReply));
                while (memory.size() > MEMORY_MAX) {
                    memory.remove(0);
                }
            }
        } catch (AIException e) {
            deliverResult(server, r -> broadcastChatReply(botName,
                    "Gagal: " + (e.getMessage() != null ? e.getMessage() : e.toString())), null);
        }
    }

    /**
     * Membuat bot berkata di chat. Harus dipanggil di server thread;
     * deliverResult sudah memindahkan eksekusi ke server thread.
     */
    private static void broadcastChatReply(String botName, String reply) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        if (bot != null && reply != null && !reply.isEmpty()) {
            bot.aiChat(reply);
        }
    }

    /**
     * Menjalankan tool di server thread (karena tool memutasi state bot).
     * Jika server null, dieksekusi langsung di thread kontrol (best-effort).
     */
    private static String executeToolOnServer(MinecraftServer server, BotBrain bot, AIToolCall toolCall) {
        if (server == null) {
            return truncateToolResult(MinecraftToolManager.instance.executeTool(
                    toolCall.name, AITool.from(toolCall.arguments), bot));
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        Bukkit.getScheduler().runTask(CarpetPlayersPlugin.instance, () -> {
            try {
                result[0] = MinecraftToolManager.instance.executeTool(
                        toolCall.name, AITool.from(toolCall.arguments), bot);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(TOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return "Tool " + toolCall.name + " timeout (" + TOOL_TIMEOUT_SECONDS + " detik)";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Tool " + toolCall.name + " dibatalkan (interrupt)";
        }
        return truncateToolResult(result[0] != null ? result[0] : "Tool " + toolCall.name + " tidak mengembalikan hasil");
    }

    private static String truncateToolResult(String s) {
        if (s != null && s.length() > MAX_TOOL_RESULT_CHARS) {
            return s.substring(0, MAX_TOOL_RESULT_CHARS) + "...(terpotong)";
        }
        return s;
    }

    private static String truncateReply(String s) {
        if (s != null && s.length() > MAX_REPLY_CHARS) {
            return s.substring(0, MAX_REPLY_CHARS) + "...(terpotong)";
        }
        return s;
    }

    private static void deliverResult(MinecraftServer server, Consumer<String> callback, String message) {
        if (callback == null) {
            return;
        }
        final String msg = message != null ? message : "";
        if (server != null) {
            Bukkit.getScheduler().runTask(CarpetPlayersPlugin.instance, () -> callback.accept(msg));
        } else {
            callback.accept(msg);
        }
    }
}