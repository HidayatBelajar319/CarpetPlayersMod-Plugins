package com.carpetplayers.ai;

import org.bukkit.command.CommandSender;

/**
 * Handler statis untuk sub-perintah AI. Dipanggil oleh BotManager (Bukkit
 * CommandExecutor) — hasil dikirim langsung ke CommandSender.
 */
public final class AICommands {

    private AICommands() {
    }

    public static AIProviderManager manager() {
        AIProviderManager manager = AIProviderManager.instance();
        manager.ensureLoaded();
        return manager;
    }

    public static void handleStart(CommandSender sender) {
        manager().setEnabled(true);
        sender.sendMessage("[AI] AI diaktifkan. Ketik /carpetplayers ai test untuk mengecek koneksi.");
    }

    public static void handleStop(CommandSender sender) {
        manager().setEnabled(false);
        sender.sendMessage("[AI] AI dinonaktifkan.");
    }

    public static void handleReload(CommandSender sender) {
        manager().reload();
        sender.sendMessage("[AI] Konfigurasi provider dimuat ulang.");
    }

    public static void handleStatus(CommandSender sender) {
        AIProviderManager manager = manager();
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
        sender.sendMessage(sb.toString());
    }

    public static void handleTest(CommandSender sender) {
        manager().testProvidersAsync(result -> {
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (!line.isEmpty()) {
                    sender.sendMessage(line);
                }
            }
        });
    }

    /**
     * Menjalankan AIController secara async (thread terpisah) dan mengirim
     * hasilnya kembali ke pengirim perintah.
     */
    public static void handleAct(CommandSender sender, String botName, String instruction) {
        manager();
        sender.sendMessage("[AI] Memproses instruksi untuk bot '" + botName + "'...");
        AIController.run(botName, instruction,
                result -> sender.sendMessage("[AI] " + result),
                error -> sender.sendMessage("[AI] Gagal: " + error));
    }

    public static void handleChat(CommandSender sender, boolean enabled) {
        manager().setChatEnabled(enabled);
        sender.sendMessage("[AI] Chat AI " + (enabled ? "diaktifkan" : "dinonaktifkan") + ".");
    }

    public static void handleForget(CommandSender sender, String botName) {
        AIController.clearMemory(botName);
        sender.sendMessage("[AI] Memori percakapan bot '" + botName + "' dibersihkan.");
    }

    public static void handleDefensive(CommandSender sender, boolean enabled) {
        manager().setDefensiveEnabled(enabled);
        sender.sendMessage("[AI] Defensive AI " + (enabled ? "diaktifkan" : "dinonaktifkan") + ".");
    }

    public static void handleProviderKey(CommandSender sender, String type, String apiKey) {
        String message = manager().setProviderApiKey(type, apiKey);
        if (!message.startsWith("[AI]")) {
            message = "[AI] " + message;
        }
        sender.sendMessage(message);
    }
}