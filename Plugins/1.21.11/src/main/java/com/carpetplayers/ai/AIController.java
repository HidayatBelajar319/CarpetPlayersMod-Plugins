package com.carpetplayers.ai;

import com.carpetplayers.CarpetPlayersPlugin;
import com.carpetplayers.bot.BotBrain;
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
     * Runs the AI tool-calling loop for a bot.
     * The whole process runs on a separate daemon thread so it never blocks the
     * server thread. All bot mutations and callbacks are invoked on the server thread.
     */
    public static void run(String botName, String instruction, Consumer<String> onResult, Consumer<String> onError) {
        Thread controlThread = new Thread(
                () -> runOnControlThread(botName, instruction, onResult, onError),
                "carpetplayers-ai-control");
        controlThread.setDaemon(true);
        controlThread.start();
    }

    /**
     * Runs an AI conversation for a bot (with per-bot memory).
     * Runs on a separate daemon thread; all bot mutations and broadcasts are
     * invoked on the server thread.
     */
    public static void runChat(String botName, String instruction) {
        Thread controlThread = new Thread(
                () -> runChatOnControlThread(botName, instruction),
                "carpetplayers-ai-chat");
        controlThread.setDaemon(true);
        controlThread.start();
    }

    /**
     * Clears a bot's conversation memory. No-op if the bot does not exist.
     * Safe to call from the server thread (uses ConcurrentHashMap).
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
        boolean serverOnline = bot != null && bot.getBot().getServer() != null;

        if (serverOnline) {
            AIProviderManager.instance().ensureLoaded();
        }

        if (bot == null) {
            deliverResult(serverOnline, onError, "Bot " + botName + " not found");
            return;
        }
        UUID key = bot.getUuid();
        List<AIMessage> memory = actMemories.computeIfAbsent(key,
                k -> Collections.synchronizedList(new ArrayList<AIMessage>()));
        List<AIMessage> messages = new ArrayList<>(memory);
        if (memory.isEmpty()) {
            messages.add(AIMessage.user("Bot: " + bot.aiGetStateInfo() + "\nPlayer instruction: " + instruction));
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
                    deliverResult(serverOnline, onError, "AI response is empty");
                    return;
                }
                lastContent = response.content != null ? response.content : "";
                if (response.toolCalls == null || response.toolCalls.isEmpty()) {
                    finalReply = lastContent;
                    break;
                }
                messages.add(AIMessage.assistantWithTools(lastContent, response.toolCalls));
                for (AIToolCall toolCall : response.toolCalls) {
                    String result = executeToolOnServer(serverOnline, bot, toolCall);
                    messages.add(AIMessage.tool(toolCall.id, result));
                }
            }
            if (finalReply.isEmpty()) {
                finalReply = lastContent.isEmpty()
                        ? "Finished (max tool-call iterations reached)" : lastContent;
            }
            synchronized (memory) {
                memory.add(AIMessage.user(instruction));
                memory.add(AIMessage.assistant(finalReply));
                while (memory.size() > MEMORY_MAX) {
                    memory.remove(0);
                }
            }
            deliverResult(serverOnline, onResult, finalReply);
        } catch (AIException e) {
            deliverResult(serverOnline, onError,
                    e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static void runChatOnControlThread(String botName, String instruction) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        if (bot == null) {
            return;
        }
        boolean serverOnline = bot.getBot().getServer() != null;
        if (serverOnline) {
            AIProviderManager.instance().ensureLoaded();
        }

        if (!AIProviderManager.instance().isEnabled()) {
            deliverResult(serverOnline, reply -> broadcastChatReply(botName, "AI is disabled. Type /carpetplayers ai start"), null);
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
                    deliverResult(serverOnline, r -> broadcastChatReply(botName, "AI response is empty"), null);
                    return;
                }
                reply = response.content != null ? response.content : "";
                if (response.toolCalls == null || response.toolCalls.isEmpty()) {
                    break;
                }
                messages.add(AIMessage.assistantWithTools(reply, response.toolCalls));
                for (AIToolCall toolCall : response.toolCalls) {
                    String result = executeToolOnServer(serverOnline, bot, toolCall);
                    messages.add(AIMessage.tool(toolCall.id, result));
                }
            }
            String finalReply = truncateReply(reply.isEmpty()
                    ? "Finished (max tool-call iterations reached)" : reply);
            deliverResult(serverOnline, r -> broadcastChatReply(botName, r), finalReply);
            synchronized (memory) {
                memory.add(AIMessage.user(instruction));
                memory.add(AIMessage.assistant(finalReply));
                while (memory.size() > MEMORY_MAX) {
                    memory.remove(0);
                }
            }
        } catch (AIException e) {
            deliverResult(serverOnline, r -> broadcastChatReply(botName,
                    "Failed: " + (e.getMessage() != null ? e.getMessage() : e.toString())), null);
        }
    }

    /**
     * Makes the bot speak in chat. Must be called on the server thread;
     * deliverResult already moves execution to the server thread.
     */
    private static void broadcastChatReply(String botName, String reply) {
        BotBrain bot = MinecraftToolManager.findBotByName(botName);
        if (bot != null && reply != null && !reply.isEmpty()) {
            bot.aiChat(reply);
        }
    }

    /**
     * Executes a tool on the server thread (because tools mutate bot state).
     * If the server is null, it is executed directly on the control thread (best-effort).
     */
    private static String executeToolOnServer(boolean serverOnline, BotBrain bot, AIToolCall toolCall) {
        if (!serverOnline) {
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
                return "Tool " + toolCall.name + " timeout (" + TOOL_TIMEOUT_SECONDS + " seconds)";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Tool " + toolCall.name + " cancelled (interrupt)";
        }
        return truncateToolResult(result[0] != null ? result[0] : "Tool " + toolCall.name + " returned no result");
    }

    private static String truncateToolResult(String s) {
        if (s != null && s.length() > MAX_TOOL_RESULT_CHARS) {
            return s.substring(0, MAX_TOOL_RESULT_CHARS) + "...(truncated)";
        }
        return s;
    }

    private static String truncateReply(String s) {
        if (s != null && s.length() > MAX_REPLY_CHARS) {
            return s.substring(0, MAX_REPLY_CHARS) + "...(truncated)";
        }
        return s;
    }

    private static void deliverResult(boolean serverOnline, Consumer<String> callback, String message) {
        if (callback == null) {
            return;
        }
        final String msg = message != null ? message : "";
        if (serverOnline) {
            Bukkit.getScheduler().runTask(CarpetPlayersPlugin.instance, () -> callback.accept(msg));
        } else {
            callback.accept(msg);
        }
    }
}
