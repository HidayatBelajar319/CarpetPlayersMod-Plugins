package com.carpetplayers.ai;

import com.carpetplayers.CarpetPlayersPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AIProviderManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AIProviderManager instance;
    private final List<AIProvider> providers = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "carpetplayers-ai");
        t.setDaemon(true);
        return t;
    });
    private File configFile;
    private AIConfig config = new AIConfig();

    private AIProviderManager() {
    }

    public static synchronized AIProviderManager instance() {
        if (instance == null) {
            instance = new AIProviderManager();
        }
        return instance;
    }

    public void ensureLoaded() {
        if (configFile != null) {
            return;
        }
        File aiDir = new File(CarpetPlayersPlugin.instance.getDataFolder(), "minecraft-ai");
        configFile = new File(aiDir, "providers.json");
        load();
    }

    public void load() {
        if (configFile == null) {
            return;
        }
        try {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    AIConfig loaded = GSON.fromJson(reader, AIConfig.class);
                    if (loaded != null) {
                        config = loaded;
                    }
                }
            } else {
                saveDefaultConfig();
            }
        } catch (Exception e) {
            CarpetPlayersPlugin.logError("Failed to load AI provider config", e);
        }
        rebuildProviders();
    }

    public void reload() {
        load();
    }

    public void save() {
        if (configFile == null) {
            return;
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            CarpetPlayersPlugin.logError("Failed to save AI provider config", e);
        }
    }

    private void saveDefaultConfig() {
        if (config.providers.isEmpty()) {
            config.providers.add(exampleProvider("openrouter", "OpenRouter", 1));
            config.providers.add(exampleProvider("gemini", "Gemini", 2));
            config.providers.add(exampleProvider("openai", "OpenAI", 3));
        }
        save();
    }

    private ProviderConfig exampleProvider(String type, String name, int priority) {
        ProviderConfig provider = defaultProvider(type);
        if (provider == null) {
            provider = new ProviderConfig();
            provider.type = "openai";
            provider.model = "gpt-4o-mini";
        }
        provider.name = name;
        provider.priority = priority;
        return provider;
    }

    /**
     * Membuat ProviderConfig dengan default (nama, baseUrl, model, models) untuk
     * tipe provider yang dikenal. Mengembalikan null jika tipe tidak dikenal.
     * priority/enabled memakai default ProviderConfig (10 / true).
     */
    private ProviderConfig defaultProvider(String type) {
        String t = type != null ? type.toLowerCase().trim() : "";
        ProviderConfig provider = new ProviderConfig();
        provider.type = t;
        switch (t) {
            case "openai":
                provider.name = "OpenAI";
                provider.baseUrl = "https://api.openai.com/v1";
                provider.model = "gpt-4o-mini";
                provider.models.add("gpt-4o-mini");
                provider.models.add("gpt-4o");
                return provider;
            case "gemini":
                provider.name = "Gemini";
                provider.baseUrl = "https://generativelanguage.googleapis.com";
                provider.model = "gemini-2.0-flash";
                provider.models.add("gemini-2.0-flash");
                provider.models.add("gemini-1.5-flash");
                return provider;
            case "openrouter":
                provider.name = "OpenRouter";
                provider.baseUrl = "https://openrouter.ai/api/v1";
                provider.model = "openrouter/auto";
                provider.models.add("openrouter/auto");
                return provider;
            case "groq":
                provider.name = "Groq";
                provider.baseUrl = "https://api.groq.com/openai/v1";
                provider.model = "llama-3.1-8b-instant";
                provider.models.add("llama-3.1-8b-instant");
                provider.models.add("llama-3.3-70b-versatile");
                return provider;
            case "orcarouter":
                provider.name = "OrcaRouter";
                provider.baseUrl = "https://api.orcarouter.ai/v1";
                provider.model = "orcarouter/auto";
                provider.models.add("orcarouter/auto");
                return provider;
            case "local":
                provider.name = "Local (Ollama)";
                provider.baseUrl = "http://localhost:11434/v1";
                provider.model = "llama3.1";
                provider.models.add("llama3.1");
                provider.models.add("qwen2.5");
                return provider;
            default:
                return null;
        }
    }

    private void rebuildProviders() {
        providers.clear();
        for (ProviderConfig providerConfig : config.providers) {
            if (!providerConfig.enabled) {
                continue;
            }
            AIProvider provider = createProvider(providerConfig);
            if (provider != null) {
                providers.add(provider);
            }
        }
        providers.sort(Comparator.comparingInt(AIProvider::getPriority));
    }

    private AIProvider createProvider(ProviderConfig providerConfig) {
        String type = providerConfig.type != null ? providerConfig.type.toLowerCase() : "openai";
        switch (type) {
            case "gemini":
                return new GeminiProvider(providerConfig);
            case "openai":
            case "openrouter":
            case "groq":
            case "compatible":
            case "local":
            default:
                return new OpenAICompatibleProvider(providerConfig);
        }
    }

    public boolean isEnabled() {
        return config.enabled;
    }

    public void setEnabled(boolean enabled) {
        config.enabled = enabled;
        save();
    }

    public boolean isChatEnabled() {
        return config.aiChatEnabled;
    }

    public void setChatEnabled(boolean enabled) {
        config.aiChatEnabled = enabled;
        save();
    }

    public boolean isDefensiveEnabled() {
        return config.aiDefensiveEnabled;
    }

    public void setDefensiveEnabled(boolean enabled) {
        config.aiDefensiveEnabled = enabled;
        save();
    }

    /**
     * Menetapkan API key untuk tipe provider. Jika provider sudah terdaftar,
     * hanya diperbarui; jika belum, dibuat dengan default (nama/baseUrl/model)
     * lalu ditambahkan. Provider langsung aktif kembali (rebuildProviders).
     * Mengembalikan pesan status siap ditampilkan ke pemain.
     */
    public String setProviderApiKey(String type, String apiKey) {
        if (type == null || type.trim().isEmpty()) {
            return "Tipe provider tidak valid.";
        }
        String normalizedType = type.trim().toLowerCase();
        for (ProviderConfig providerConfig : config.providers) {
            if (providerConfig.type != null
                    && providerConfig.type.trim().equalsIgnoreCase(normalizedType)) {
                providerConfig.apiKey = apiKey != null ? apiKey : "";
                save();
                rebuildProviders();
                return "[AI] API key untuk " + providerConfig.name + " diperbarui. Model: "
                        + providerConfig.model + ".";
            }
        }
        ProviderConfig provider = defaultProvider(normalizedType);
        if (provider == null) {
            return "Tipe provider tidak dikenal: " + type
                    + " (opsi: openai, gemini, openrouter, groq, local).";
        }
        provider.apiKey = apiKey != null ? apiKey : "";
        config.providers.add(provider);
        save();
        rebuildProviders();
        return "[AI] Provider " + provider.name + " ditambahkan. API key tersimpan. Model default: "
                + provider.model + ".";
    }

    public String getSystemPrompt() {
        return config.systemPrompt != null ? config.systemPrompt : "";
    }

    public void setSystemPrompt(String prompt) {
        config.systemPrompt = prompt;
        save();
    }

    public List<AIProvider> getProviders() {
        return new ArrayList<>(providers);
    }

    /**
     * Memastikan pesan system (dari config) ada di depan daftar pesan.
     * Jika pemanggil sudah menyertakan pesan system sendiri, tidak ditambah ulang.
     */
    public List<AIMessage> withSystemPrompt(List<AIMessage> messages) {
        if (messages != null && !messages.isEmpty() && "system".equals(messages.get(0).role)) {
            return messages;
        }
        List<AIMessage> result = new ArrayList<>();
        String prompt = getSystemPrompt();
        if (prompt != null && !prompt.isEmpty()) {
            result.add(AIMessage.system(prompt));
        }
        if (messages != null) {
            result.addAll(messages);
        }
        return result;
    }

    public List<ProviderHealth> getHealth() {
        List<ProviderHealth> health = new ArrayList<>();
        for (AIProvider provider : providers) {
            health.add(provider.getHealth());
        }
        return health;
    }

    public AIProvider getActiveProvider() {
        for (AIProvider provider : providers) {
            if (provider.isEnabled() && !provider.onCooldown() && !provider.getModels().isEmpty()) {
                return provider;
            }
        }
        return null;
    }

    public String getActiveModel(AIProvider provider) {
        if (provider == null) {
            return "";
        }
        List<String> models = provider.getModels();
        return models.isEmpty() ? "" : models.get(0);
    }

    public AIResponse sendMessage(List<AIMessage> messages) throws AIException {
        return sendMessageInternal(messages, null);
    }

    public AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools) throws AIException {
        return sendMessageInternal(messages, tools);
    }

    private AIResponse sendMessageInternal(List<AIMessage> messages, List<AITool> tools) throws AIException {
        if (!config.enabled) {
            throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
                    "AI is disabled (/carpetplayers ai start to enable)");
        }
        messages = withSystemPrompt(messages);
        boolean useTools = tools != null && !tools.isEmpty();
        for (AIProvider provider : providers) {
            if (!provider.isEnabled() || provider.onCooldown()) {
                continue;
            }
            List<String> models = provider.getModels();
            if (models.isEmpty()) {
                continue;
            }
            for (String model : models) {
                try {
                    AIResponse response = useTools
                            ? provider.sendMessageWithTools(messages, tools, model)
                            : provider.sendMessage(messages, model);
                    provider.markSuccess();
                    return response;
                } catch (AIException e) {
                    provider.markFailure(e);
                    CarpetPlayersPlugin.log("AI provider " + provider.getName() + " model " + model
                            + " failed: " + e.getMessage());
                }
            }
        }
        throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
                "No AI provider/model available");
    }

    public void sendMessageAsync(List<AIMessage> messages, Consumer<AIResponse> onSuccess,
                                 Consumer<AIException> onError) {
        executor.submit(() -> {
            try {
                AIResponse response = sendMessage(messages);
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
            } catch (AIException e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        });
    }

    public void sendMessageWithToolsAsync(List<AIMessage> messages, List<AITool> tools,
                                          Consumer<AIResponse> onSuccess,
                                          Consumer<AIException> onError) {
        executor.submit(() -> {
            try {
                AIResponse response = sendMessageWithTools(messages, tools);
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
            } catch (AIException e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        });
    }

    public void testProvidersAsync(Consumer<String> onResult) {
        executor.submit(() -> {
            StringBuilder sb = new StringBuilder();
            if (providers.isEmpty()) {
                sb.append("Tidak ada provider terkonfigurasi. Edit config/minecraft-ai/providers.json");
            } else {
                for (AIProvider provider : providers) {
                    String status;
                    if (provider.getModels().isEmpty()) {
                        status = "tidak ada model";
                    } else {
                        long start = System.currentTimeMillis();
                        boolean ok = provider.testConnection();
                        long elapsed = System.currentTimeMillis() - start;
                        status = (ok ? "OK (" : "GAGAL (") + elapsed + " ms)";
                        if (ok) {
                            provider.markSuccess();
                        } else {
                            provider.markFailure(new AIException(AIException.ErrorType.UNKNOWN,
                                    provider.getName(), null, 0,
                                    provider.getHealth().lastError != null ? provider.getHealth().lastError : "test failed"));
                        }
                    }
                    sb.append(provider.getName()).append(" [").append(provider.getType()).append("]: ")
                            .append(status).append("\n");
                }
            }
            onResult.accept(sb.toString());
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
