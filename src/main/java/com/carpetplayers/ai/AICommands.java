package com.carpetplayers.ai;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AICommands {

    private AICommands() {
    }

    public static AIProviderManager manager(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = AIProviderManager.instance();
        manager.ensureLoaded();
        return manager;
    }

    public static int start(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        manager.setEnabled(true);
        context.getSource().sendSuccess(new TextComponent("[AI] AI diaktifkan. Ketik /carpetplayers ai test untuk mengecek koneksi."), false);
        return 1;
    }

    public static int stop(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        manager.setEnabled(false);
        context.getSource().sendSuccess(new TextComponent("[AI] AI dinonaktifkan."), false);
        return 1;
    }

    public static int reload(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        manager.reload();
        context.getSource().sendSuccess(new TextComponent("[AI] Konfigurasi provider dimuat ulang."), false);
        return 1;
    }

    public static int status(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        StringBuilder sb = new StringBuilder();
        sb.append("[AI] Status: ").append(manager.isEnabled() ? "AKTIF" : "MATI").append("\n");
        for (AIProvider provider : manager.getProviders()) {
            ProviderHealth health = provider.getHealth();
            sb.append("  - ").append(provider.getName())
                    .append(" [").append(provider.getType()).append("] prio ").append(provider.getPriority())
                    .append(": ").append(health.enabled ? "enabled" : "disabled")
                    .append(health.onCooldown ? " (cooldown)" : "")
                    .append(" | model: ").append(provider.getModels())
                    .append("\n");
        }
        sendMessage(context, sb.toString());
        return 1;
    }

    public static int test(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        source.sendSuccess(new TextComponent("[AI] Mengetes semua provider..."), false);
        manager.testProvidersAsync(result -> server.execute(() -> {
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (!line.isEmpty()) {
                    sendMessage(context, line);
                }
            }
        }));
        return 1;
    }

    /**
     * Handler untuk subcommand /carpetplayers ai act &lt;botname&gt; &lt;instruction&gt;.
     * Menjalankan AIController secara async (thread terpisah) dan mengirim hasilnya
     * kembali ke pengirim perintah.
     */
    public static int act(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String botName = StringArgumentType.getString(context, "botname");
        String instruction = StringArgumentType.getString(context, "instruction");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        manager(context);
        source.sendSuccess(new TextComponent("[AI] Memproses instruksi untuk bot '" + botName + "'..."), false);
        AIController.run(botName, instruction,
                result -> {
                    if (server != null) {
                        server.execute(() -> sendMessage(context, "[AI] " + result));
                    } else {
                        sendMessage(context, "[AI] " + result);
                    }
                },
                error -> {
                    if (server != null) {
                        server.execute(() -> sendMessage(context, "[AI] Gagal: " + error));
                    } else {
                        sendMessage(context, "[AI] Gagal: " + error);
                    }
                });
        return 1;
    }

    /**
     * Handler untuk /carpetplayers ai chat &lt;enabled&gt;:
     * mengaktifkan/menonaktifkan balasan chat AI per bot.
     */
    public static int chat(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        manager.setChatEnabled(enabled);
        context.getSource().sendSuccess(new TextComponent(
                "[AI] Chat AI " + (enabled ? "diaktifkan" : "dinonaktifkan") + "."), false);
        return 1;
    }

    /**
     * Handler untuk /carpetplayers ai forget &lt;botname&gt;:
     * membersihkan memori percakapan sebuah bot.
     */
    public static int forget(CommandContext<CommandSourceStack> context) {
        String botName = StringArgumentType.getString(context, "botname");
        AIController.clearMemory(botName);
        context.getSource().sendSuccess(new TextComponent(
                "[AI] Memori percakapan bot '" + botName + "' dibersihkan."), false);
        return 1;
    }

    /**
     * Handler untuk /carpetplayers ai defensive &lt;enabled&gt;:
     * mengaktifkan/menonaktifkan defensive AI (anti-grief / anti-serangan).
     */
    public static int defensive(CommandContext<CommandSourceStack> context) {
        AIProviderManager manager = manager(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        manager.setDefensiveEnabled(enabled);
        context.getSource().sendSuccess(new TextComponent(
                "[AI] Defensive AI " + (enabled ? "diaktifkan" : "dinonaktifkan") + "."), false);
        return 1;
    }

    /**
     * Handler untuk perintah set API key provider (misal
     * /carpetplayers ai key &lt;type&gt; &lt;apikey&gt;). `type` diteruskan dari wiring
     * perintah (satu literal per tipe provider).
     */
    public static int providerKey(CommandContext<CommandSourceStack> context, String type) {
        String apiKey = StringArgumentType.getString(context, "apikey");
        AIProviderManager manager = manager(context);
        String message = manager.setProviderApiKey(type, apiKey);
        if (!message.startsWith("[AI]")) {
            message = "[AI] " + message;
        }
        context.getSource().sendSuccess(new TextComponent(message), false);
        return 1;
    }

    private static void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer) {
            source.getEntity().sendMessage(new TextComponent(message), source.getEntity().getUUID());
        } else {
            source.sendSuccess(new TextComponent(message), false);
        }
    }
}
