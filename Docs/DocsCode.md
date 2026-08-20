# CarpetPlayers — Code Documentation (Fabric 1.16.5 Mod)

> Source: [src/](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/src)

This document covers the first half of the codebase: build & config files, the mod entry point, the complete `ai/` package, the `bot/` package, the `config/` package, and the `mixin/` package. The mod spawns intelligent FakePlayer bots via the Carpet mod API, gives them an AI "brain" backed by pluggable LLM providers (OpenAI-compatible / Gemini) with tool calling, and supports advanced PvP, kit, and interactive chat behavior.

## Project Structure

```
CarpetPlayersMod/
├── build.gradle                        # Fabric-Loom build configuration
├── settings.gradle                     # Gradle plugin repositories & project name
├── gradle.properties                   # MC / loader / dependency versions
├── src/main/
│   ├── java/com/carpetplayers/
│   │   ├── CarpetPlayersMod.java       # Mod entry point (ModInitializer)
│   │   ├── ai/                         # AI provider + tool-calling subsystem
│   │   │   ├── AIController.java       # Async tool-calling loop
│   │   │   ├── AIProviderManager.java  # Provider registry + failover
│   │   │   ├── AIConfig.java           # JSON-serialised AI settings
│   │   │   ├── AICommands.java         # /carpetplayers ai <sub> handlers
│   │   │   ├── MinecraftToolManager.java  # Bot tools exposed to the LLM
│   │   │   ├── AITool.java             # Tool model + JSON-Schema helpers
│   │   │   ├── AIMessage.java          # Chat message role/content DTO
│   │   │   ├── AIToolCall.java         # Tool-call DTO
│   │   │   ├── AIResponse.java         # Provider response DTO
│   │   │   ├── AIException.java        # Typed AI error hierarchy
│   │   │   ├── AIProvider.java         # Provider interface
│   │   │   ├── AbstractAIProvider.java # HTTP plumbing + health/cooldown
│   │   │   ├── GeminiProvider.java     # Google Gemini implementation
│   │   │   ├── OpenAICompatibleProvider.java  # OpenAI/OpenRouter/Groq/Local
│   │   │   ├── ProviderConfig.java     # Per-provider config DTO
│   │   │   └── ProviderHealth.java     # Health snapshot DTO
│   │   ├── bot/                        # FakePlayer brain & management
│   │   │   ├── BotBrain.java           # Bot state machine + tick logic
│   │   │   ├── BotManager.java         # Spawn/tick/commands + registries
│   │   │   ├── PvPBot.java             # PvP-specialised BotBrain subclass
│   │   │   └── KitManager.java         # PvP kit equipment system
│   │   ├── config/
│   │   │   └── ModConfig.java          # General mod config (JSON)
│   │   └── mixin/
│   │       ├── LivingEntityMixin.java              # Damage reaction hook
│   │       └── ServerGamePacketListenerImplMixin.java  # Chat handling hook
│   └── resources/
│       ├── fabric.mod.json             # Fabric mod descriptor
│       └── carpetplayers.mixins.json   # Mixin registration
```

---

## Build & Configuration

### build.gradle

🔗 [build.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/build.gradle)

Fabric-Loom build script. Declares the Minecraft, Fabric Loader, Fabric API, and Carpet mod dependencies (Carpet is required for the FakePlayer system), sets Java 8 compatibility, enables source jars, and expands the `${version}` placeholder in `fabric.mod.json` during resource processing.

| Member | Type | Description |
|---|---|---|
| `net.fabricmc.fabric-loom-remap` | plugin | Loom Gradle plugin with remapping support |
| `version` | property | From `gradle.properties` (`mod_version`) |
| `masa.dy.fi/maven` | repository | Hosts the Carpet mod (`fabric-carpet`) |
| `processResources` | task | Expands `version` into `fabric.mod.json` |
| `JavaCompile.options.release` | config | Targets Java 8 bytecode |
| `withSourcesJar()` | task | Attaches a sources jar to the build |

```gradle
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    // CARPET DEPENDENCIES - REQUIRED (FakePlayer system)
    modImplementation "carpet:fabric-carpet:${project.carpet_version}"
}
```

### settings.gradle

🔗 [settings.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/settings.gradle)

Standard Gradle settings file. Configures the plugin management repositories (Fabric maven, Maven Central, Gradle plugin portal) and names the root project `carpetplayers`.

| Member | Type | Description |
|---|---|---|
| `pluginManagement.repositories` | block | Where buildscript plugins are resolved from |
| `rootProject.name` | property | Project name: `carpetplayers` |

### gradle.properties

🔗 [gradle.properties](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/gradle.properties)

Central version catalogue. Pins Minecraft `1.16.5`, Fabric Loader `0.19.3`, Loom `1.17-SNAPSHOT`, Fabric API `0.42.0+1.16`, and the required Carpet build `1.16.5-1.4.44+v210714`.

| Property | Value | Description |
|---|---|---|
| `minecraft_version` | `1.16.5` | Target Minecraft version |
| `loader_version` | `0.19.3` | Fabric Loader version |
| `loom_version` | `1.17-SNAPSHOT` | Fabric Loom version |
| `mod_version` | `1.0.0` | Mod version |
| `maven_group` | `com.carpetplayers` | Maven group |
| `archives_base_name` | `carpetplayers` | Jar base name |
| `fabric_api_version` | `0.42.0+1.16` | Fabric API version |
| `carpet_version` | `1.16.5-1.4.44+v210714` | Carpet mod version |

### src/main/resources/fabric.mod.json

🔗 [fabric.mod.json](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/resources/fabric.mod.json)

Fabric mod descriptor. Declares the mod id `carpetplayers`, its main entrypoint, the mixin configuration, and hard dependencies on Fabric Loader, Fabric API, Minecraft `1.16.x`, and the Carpet mod.

| Field | Value | Description |
|---|---|---|
| `id` | `carpetplayers` | Mod identifier |
| `version` | `${version}` | Expanded at build time from `gradle.properties` |
| `entrypoints.main` | `com.carpetplayers.CarpetPlayersMod` | Mod initializer class |
| `mixins` | `["carpetplayers.mixins.json"]` | Mixin config reference |
| `depends.carpet` | `"*"` | Carpet mod is a hard requirement |
| `recommends.carpet-extra` | `"*"` | Optional companion mod |

```json
{
  "id": "carpetplayers",
  "version": "${version}",
  "name": "Carpet Players",
  "entrypoints": { "main": ["com.carpetplayers.CarpetPlayersMod"] },
  "mixins": ["carpetplayers.mixins.json"],
  "depends": {
    "fabricloader": ">=0.7.1", "fabric": "*", "minecraft": "1.16.x", "carpet": "*"
  }
}
```

### src/main/resources/carpetplayers.mixins.json

🔗 [carpetplayers.mixins.json](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/resources/carpetplayers.mixins.json)

Mixin registration file. Loads the two mixins in package `com.carpetplayers.mixin` targeting Java 8 compatibility, and requires at least one successful injection per callback (`defaultRequire: 1`).

| Field | Value | Description |
|---|---|---|
| `required` | `true` | Mixins must apply, else crash |
| `package` | `com.carpetplayers.mixin` | Mixin class package |
| `compatibilityLevel` | `JAVA_8` | Bytecode level for mixin processing |
| `mixins` | `ServerGamePacketListenerImplMixin`, `LivingEntityMixin` | Classes to apply |
| `injectors.defaultRequire` | `1` | Minimum successful injections |

---

## Entry Point

### CarpetPlayersMod.java

🔗 [CarpetPlayersMod.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/CarpetPlayersMod.java)

The Fabric mod initializer. Loads both configs immediately, registers the `/carpetplayers` command tree and the server-tick handler through `BotManager`, and installs a shutdown hook that shuts down the AI thread pools.

| Member | Type | Description |
|---|---|---|
| `MOD_ID` | `String` | `"carpetplayers"` |
| `LOGGER` | `Logger` | Log4j logger for the mod |
| `onInitialize()` | method | Entry point; wires configs, commands, ticks, shutdown |
| `CommandRegistrationCallback.EVENT` | event | Registers `BotManager::registerCommands` |
| `ServerTickEvents.END_SERVER_TICK` | event | Registers `BotManager::tick` |

```java
@Override
public void onInitialize() {
    String envType = FabricLoader.getInstance().getEnvironmentType().name();
    LOGGER.info("Carpet Players Mod initializing... Environment: {}", envType);

    ModConfig.ensureLoaded();
    AIProviderManager.instance().ensureLoaded();

    CommandRegistrationCallback.EVENT.register(BotManager::registerCommands);
    ServerTickEvents.END_SERVER_TICK.register(BotManager::tick);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        AIController.shutdown();
        AIProviderManager.instance().shutdown();
        LOGGER.info("Carpet Players Mod shut down.");
    }, "carpetplayers-shutdown"));
}
```

---

## AI Package

### AIController.java

🔗 [AIController.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIController.java)

The async "brain" that runs the LLM tool-calling loop for a bot. Work runs on a daemon thread pool while every bot mutation, broadcast, and callback is marshalled back onto the server thread (via `server.execute`), keeping the server safe. Maintains per-bot chat/act memories and truncates oversized replies/tool results.

| Member | Type | Description |
|---|---|---|
| `MAX_ITERATIONS` | `int` | `6` — max tool-call loop rounds |
| `TOOL_TIMEOUT_SECONDS` | `int` | `5` — per-tool server-side execution timeout |
| `MAX_MESSAGES` / `MEMORY_MAX` | `int` | `24` / `12` — message & memory caps |
| `MAX_TOOL_RESULT_CHARS` / `MAX_REPLY_CHARS` | `int` | `800` / `500` truncation limits |
| `executor` | `ExecutorService` | Daemon cached thread pool (`carpetplayers-ai`) |
| `run(...)` | method | Starts act loop with result/error callbacks |
| `runChat(...)` | method | Starts chat loop (per-bot memory) |
| `clearMemory(botName)` | method | Wipes a bot's conversation memory |
| `executeToolOnServer(...)` | method | Runs a tool via `server.execute` + `CountDownLatch` |
| `shutdown()` | method | `executor.shutdownNow()` |

```java
private static String executeToolOnServer(MinecraftServer server, BotBrain bot, AIToolCall toolCall) {
    if (server == null) {
        return truncateToolResult(MinecraftToolManager.instance.executeTool(
                toolCall.name, AITool.from(toolCall.arguments), bot));
    }
    final CountDownLatch latch = new CountDownLatch(1);
    final String[] result = new String[1];
    server.execute(() -> {
        try { result[0] = MinecraftToolManager.instance.executeTool(
                toolCall.name, AITool.from(toolCall.arguments), bot); }
        finally { latch.countDown(); }
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
```

### AIProviderManager.java

🔗 [AIProviderManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIProviderManager.java)

Singleton registry for AI providers. Loads/saves `config/minecraft-ai/providers.json`, rebuilds the provider list (sorted by priority), and implements failover: for each request it iterates providers and models until one succeeds. Also manages global toggles (enabled, chat, defensive), system prompt, API keys, health reporting, and async sends.

| Member | Type | Description |
|---|---|---|
| `instance()` | static | Lazily-created synchronized singleton |
| `ensureLoaded()` | method | Resolves config file & calls `load()` |
| `load()` / `reload()` | method | Read config JSON; rebuild providers |
| `saveDefaultConfig()` | method | Seeds example openrouter/gemini/openai providers |
| `defaultProvider(type)` | method | Returns defaults for known provider types |
| `rebuildProviders()` | method | Instantiates enabled providers, sorts by priority |
| `sendMessageInternal(...)` | method | Failover loop across providers/models |
| `sendMessageAsync` / `sendMessageWithToolsAsync` | method | Executor-backed async variants |
| `testProvidersAsync(onResult)` | method | Connection-tests all providers off-thread |
| `setProviderApiKey(type, key)` | method | Update or create a provider with a key |

```java
private AIResponse sendMessageInternal(List<AIMessage> messages, List<AITool> tools) throws AIException {
    if (!config.enabled) {
        throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
                "AI is disabled (/carpetplayers ai start to enable)");
    }
    messages = withSystemPrompt(messages);
    boolean useTools = tools != null && !tools.isEmpty();
    for (AIProvider provider : providers) {
        if (!provider.isEnabled() || provider.onCooldown()) continue;
        for (String model : provider.getModels()) {
            try {
                AIResponse response = useTools
                        ? provider.sendMessageWithTools(messages, tools, model)
                        : provider.sendMessage(messages, model);
                provider.markSuccess();
                return response;
            } catch (AIException e) {
                provider.markFailure(e);
                CarpetPlayersMod.LOGGER.warn("AI provider {} model {} failed: {}",
                        provider.getName(), model, e.getMessage());
            }
        }
    }
    throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
            "No AI provider/model available");
}
```

### AIConfig.java

🔗 [AIConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIConfig.java)

Plain JSON-serialised configuration DTO for the AI subsystem. Holds global toggles, the system prompt given to the LLM, request/cooldown timings, and the list of provider configs.

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | `boolean` | `true` | Master AI switch |
| `aiChatEnabled` | `boolean` | `true` | AI chat replies on/off |
| `aiDefensiveEnabled` | `boolean` | `true` | Reaction when attacked |
| `systemPrompt` | `String` | long default | System prompt describing bot tools/behaviour |
| `requestTimeoutMs` | `int` | `30000` | Per-request HTTP timeout |
| `failureCooldownMs` | `int` | `30000` | Cooldown after a provider failure |
| `debugLogging` | `boolean` | `false` | Debug log output |
| `providers` | `List<ProviderConfig>` | empty | Configured providers |

```java
public class AIConfig {
    public boolean enabled = true;
    public boolean aiChatEnabled = true;
    public boolean aiDefensiveEnabled = true;
    public String systemPrompt = "You are the AI brain of a Minecraft bot in Minecraft 1.16.5 (Fabric with Carpet mod). "
            + "You control the bot's movement, actions, and interactions using tools. "
            + "...";
    public int requestTimeoutMs = 30000;
    public int failureCooldownMs = 30000;
    public boolean debugLogging = false;
    public List<ProviderConfig> providers = new ArrayList<>();
}
```

### AICommands.java

🔗 [AICommands.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AICommands.java)

Brigadier command handlers backing the `/carpetplayers ai ...` sub-tree: `start`, `stop`, `status`, `reload`, `test`, `act <bot> <instruction>`, `chat <enabled>`, `forget <bot>`, `defensive <enabled>`, and `provider <type> <apikey>`. Async results are marshalled back to the command source.

| Member | Type | Description |
|---|---|---|
| `manager(context)` | static | Ensures provider config loaded, returns manager |
| `start` / `stop` | static | Enable / disable the AI |
| `reload` | static | Reload provider config from disk |
| `status` | static | Print provider health/priority/cooldown |
| `test` | static | Async connection-test all providers |
| `act` | static | Run `AIController.run` with result/error callbacks |
| `chat` / `forget` / `defensive` | static | Toggle AI chat, clear memory, toggle defensive AI |
| `providerKey(context, type)` | static | Set an API key for a provider type |
| `sendMessage(...)` | private | Route feedback to player or console |

```java
public static int act(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    String botName = StringArgumentType.getString(context, "botname");
    String instruction = StringArgumentType.getString(context, "instruction");
    CommandSourceStack source = context.getSource();
    MinecraftServer server = source.getServer();
    manager(context);
    source.sendSuccess(new TextComponent("[AI] Processing instruction for bot '" + botName + "'..."), false);
    AIController.run(botName, instruction,
            result -> server.execute(() -> sendMessage(context, "[AI] " + result)),
            error  -> server.execute(() -> sendMessage(context, "[AI] Failed: " + error)));
    return 1;
}
```

### MinecraftToolManager.java

🔗 [MinecraftToolManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/MinecraftToolManager.java)

The tool surface exposed to the LLM. Singleton that registers 14 tools (`get_state`, `move`, `jump`, `sneak`, `look_at`, `attack`, `eat`, `chat`, `stop`, `set_state`, `mine_block`, `use_item`, `drop_item`, `equip_kit`, `run_command`) and executes them safely, never throwing — failures are returned as error strings.

| Member | Type | Description |
|---|---|---|
| `instance` | static final | Singleton instance |
| `registerDefaultTools()` | private | Registers all tools with JSON-Schema params |
| `getTools()` | method | Immutable copy of the tool list |
| `executeTool(name, args, bot)` | method | Dispatch tool; catches all exceptions |
| `findBotByName(name)` | static | Look up a `BotBrain` by bot name |
| `targetExists(name)` | private | Checks bot or real-player targets |

```java
public String executeTool(String toolName, JsonObject args, BotBrain bot) {
    AITool tool = tools.get(toolName);
    if (tool == null) {
        return "Unknown tool: " + toolName;
    }
    try {
        if (args == null) args = new JsonObject();
        return tool.execute(args, bot);
    } catch (Exception e) {
        return "Error executing tool " + toolName + ": "
                + (e.getMessage() != null ? e.getMessage() : e.toString());
    }
}
```

### AITool.java

🔗 [AITool.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AITool.java)

Data model for a tool: name, description, JSON-Schema parameters, and an executor lambda. Also provides static factory helpers that build object-style JSON-Schema parameter definitions (`stringParam`, `intParam`, `doubleParam`, `booleanParam`, `enumParam`, `noParams`) and `from(...)` to safely parse raw JSON argument strings.

| Member | Type | Description |
|---|---|---|
| `name` | `String` | Tool identifier |
| `description` | `String` | Shown to the LLM |
| `parameters` | `JsonObject` | JSON Schema for args |
| `Executor` | interface | `String execute(JsonObject args, BotBrain bot)` |
| `execute(args, bot)` | method | Delegates to the executor |
| `from(json)` | static | Parse raw JSON args (empty object on failure) |
| `objectParams(...)` | static | Build object schema with required/properties |
| `intParam` / `enumParam` / etc. | static | Typed parameter factories |

```java
public static JsonObject objectParams(JsonObject... params) {
    JsonObject result = new JsonObject();
    result.addProperty("type", "object");
    JsonObject properties = new JsonObject();
    JsonArray required = new JsonArray();
    for (JsonObject param : params) {
        if (param == null || !param.has("name") || !param.has("schema")) continue;
        String name = param.get("name").getAsString();
        properties.add(name, param.getAsJsonObject("schema"));
        if (param.has("required") && param.get("required").getAsBoolean()) required.add(name);
    }
    result.add("properties", properties);
    if (required.size() > 0) result.add("required", required);
    return result;
}
```

### AIMessage.java

🔗 [AIMessage.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIMessage.java)

Immutable chat-message DTO used in all conversations: role (`system`/`user`/`assistant`/`tool`), content, an optional tool-call id, and an optional list of tool calls attached to an assistant message. Provides static factory methods for each role.

| Member | Type | Description |
|---|---|---|
| `role` | `String` | Message role |
| `content` | `String` | Message text |
| `toolCallId` | `String` | Tool-call id (for `tool` role) |
| `toolCalls` | `List<AIToolCall>` | Calls made by the assistant |
| `system/user/assistant/tool/assistantWithTools` | static | Factory methods |

```java
public static AIMessage assistantWithTools(String content, List<AIToolCall> toolCalls) {
    return new AIMessage("assistant", content, null, toolCalls);
}
public static AIMessage tool(String toolCallId, String content) {
    return new AIMessage("tool", content, toolCallId, null);
}
```

### AIToolCall.java

🔗 [AIToolCall.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIToolCall.java)

Tiny immutable DTO describing a single tool call requested by the LLM: an id, the tool name, and the raw JSON argument string.

| Member | Type | Description |
|---|---|---|
| `id` | `String` | Tool-call id |
| `name` | `String` | Tool name to execute |
| `arguments` | `String` | Raw JSON args (parsed via `AITool.from`) |

### AIResponse.java

🔗 [AIResponse.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIResponse.java)

Immutable response DTO returned by a provider: the text content, provider name, model used, the raw JSON body (for debugging), and any tool calls requested.

| Member | Type | Description |
|---|---|---|
| `content` | `String` | Reply text |
| `providerName` | `String` | Which provider answered |
| `model` | `String` | Model used |
| `raw` | `String` | Raw response body |
| `toolCalls` | `List<AIToolCall>` | Tool calls (may be null/empty) |

### AIException.java

🔗 [AIException.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIException.java)

Typed exception hierarchy for AI failures, carrying an `ErrorType` enum, provider/model names, and the HTTP status code so callers and health tracking can react appropriately.

| Member | Type | Description |
|---|---|---|
| `ErrorType` | enum | `AUTH, RATE_LIMIT, QUOTA, NETWORK, HTTP, MODEL_NOT_FOUND, NO_PROVIDER, UNKNOWN` |
| `type` | `ErrorType` | Categorised failure kind |
| `providerName` | `String` | Failing provider |
| `model` | `String` | Failing model |
| `statusCode` | `int` | HTTP status (0 if N/A) |
| ctors | method | With/without root cause |

### AIProvider.java

🔗 [AIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AIProvider.java)

Interface every LLM provider implements. Exposes metadata (name, type, enabled, priority, models), message sending with optional tools, connection testing, health snapshots, and success/failure bookkeeping for cooldown handling.

| Member | Type | Description |
|---|---|---|
| `getName` / `getType` | method | Provider identity |
| `isEnabled` / `getPriority` / `getModels` | method | Routing metadata |
| `sendMessage(messages, model)` | method | Plain chat completion |
| `sendMessageWithTools(...)` | default | Tool-calling; default delegates to `sendMessage` |
| `testConnection()` | method | Liveness probe |
| `getHealth()` | method | `ProviderHealth` snapshot |
| `markSuccess` / `markFailure` / `onCooldown` | method | Health/cooldown state |

### AbstractAIProvider.java

🔗 [AbstractAIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/AbstractAIProvider.java)

Base class implementing shared provider behaviour: config-driven metadata, failure/cooldown tracking, health snapshots, HTTP error classification (`401/403`→AUTH, `404`→MODEL_NOT_FOUND, `429`→RATE_LIMIT, `402`→QUOTA, `5xx`→HTTP, network/timeout→NETWORK), and a generic `postJson` helper used by all concrete providers.

| Member | Type | Description |
|---|---|---|
| `config` | `ProviderConfig` | Provider settings |
| `failureCount` / `cooldownUntil` / `lastError` | volatile | Health state |
| `getApiKey` / `getBaseUrl` / `getTimeoutMs` | method | Config accessors |
| `markSuccess` / `markFailure` | method | Reset / trigger cooldown (30 s) |
| `onCooldown()` | method | `now < cooldownUntil` |
| `classifyHttpError(code, body)` | method | Maps status to `AIException` types |
| `postJson(url, jsonBody, headers)` | method | POST helper returning `HttpResult(code, body)` |
| `HttpResult` | inner class | Code + body holder |

```java
protected AIException classifyHttpError(int code, String body) {
    String reason = body != null && !body.isEmpty() ? body : "HTTP " + code;
    switch (code) {
        case 401: case 403:
            return new AIException(AIException.ErrorType.AUTH, getName(), null, code,
                    "Authentication failed (" + code + "): " + reason);
        case 429:
            return new AIException(AIException.ErrorType.RATE_LIMIT, getName(), null, code,
                    "Rate limited (" + code + "): " + reason);
        case 402:
            return new AIException(AIException.ErrorType.QUOTA, getName(), null, code,
                    "Quota exceeded (" + code + "): " + reason);
        case 500: case 502: case 503: case 504:
            return new AIException(AIException.ErrorType.HTTP, getName(), null, code,
                    "Provider server error (" + code + "): " + reason);
        default:
            return new AIException(AIException.ErrorType.HTTP, getName(), null, code,
                    "Unexpected HTTP error (" + code + "): " + reason);
    }
}
```

### GeminiProvider.java

🔗 [GeminiProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/GeminiProvider.java)

Concrete provider for Google's Gemini API. Builds `v1beta/models/<model>:generateContent` requests with function-declaration-based tool calling, translates `tool` messages into `functionResponse` parts (resolving names via a tool-call-id map), moves the system prompt into `systemInstruction`, and classifies Gemini `error.status` values into typed `AIException`s.

| Member | Type | Description |
|---|---|---|
| `buildEndpoint(model)` | private | Endpoint with `?key=<apikey>` |
| `sendMessage(messages, model)` | method | Plain generateContent |
| `sendMessageWithTools(...)` | method | Tool-calling generateContent |
| `buildGenerateContentPayload(...)` | private | Builds payload incl. `systemInstruction`, `functionCall`/`functionResponse`, `tools.functionDeclarations` |
| `classifyGeminiError(code, body)` | private | Maps `error.status` → typed exceptions |
| `testConnection()` | method | Sends `"Say: OK"` probe |

```java
@Override
public AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools, String model)
        throws AIException {
    if (tools == null || tools.isEmpty()) return sendMessage(messages, model);
    String endpoint = buildEndpoint(model);
    String json = buildGenerateContentPayload(messages, tools);
    AbstractAIProvider.HttpResult result = postJson(endpoint, json, new HashMap<>());
    if (result.code < 200 || result.code >= 300) throw classifyGeminiError(result.code, result.body);
    // parse candidates[0].content.parts -> text + functionCall entries
    ...
}
```

### OpenAICompatibleProvider.java

🔗 [OpenAICompatibleProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/OpenAICompatibleProvider.java)

Concrete provider for the OpenAI `/chat/completions` protocol, which also serves OpenRouter, Groq, "compatible", and local (Ollama) endpoints (fallback default base URLs per type). Serialises AIMessages (including `tool_call_id` and assistant `tool_calls`) into the OpenAI wire format and parses `choices[0].message` content plus `tool_calls`.

| Member | Type | Description |
|---|---|---|
| `buildEndpoint()` | private | Default base URL per provider type + `/chat/completions` |
| `sendMessage` / `sendMessageWithTools` | method | Delegate to `doSend` |
| `doSend(messages, tools, model)` | private | POST + parse; `Authorization: Bearer <key>` |
| `buildPayload(...)` | private | Builds `messages` + `tools` arrays |
| `parseToolCalls(message)` | private | Parses `tool_calls` into `AIToolCall` list |
| `testConnection()` | method | `"Say: OK"` probe |

```java
private String buildPayload(List<AIMessage> messages, List<AITool> tools, String model) {
    JsonObject payload = new JsonObject();
    payload.addProperty("model", model);
    JsonArray messageArray = new JsonArray();
    for (AIMessage msg : messages) {
        JsonObject m = new JsonObject();
        m.addProperty("role", msg.role);
        m.addProperty("content", msg.content != null ? msg.content : "");
        if ("tool".equals(msg.role) && msg.toolCallId != null)
            m.addProperty("tool_call_id", msg.toolCallId);
        if ("assistant".equals(msg.role) && msg.toolCalls != null && !msg.toolCalls.isEmpty()) {
            // adds tool_calls[] entries with id/type/function{name,arguments}
        }
        messageArray.add(m);
    }
    payload.add("messages", messageArray);
    if (tools != null && !tools.isEmpty()) {
        // adds tools[] with type=function and function{name,description,parameters}
    }
    return GSON.toJson(payload);
}
```

### ProviderConfig.java

🔗 [ProviderConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/ProviderConfig.java)

Plain DTO describing a single AI provider entry in `providers.json`. Holds identity, credentials, endpoint, model list, priority (lower = tried first), enabled flag, and timeout.

| Field | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | null | Display name |
| `type` | `String` | `"openai"` | Provider kind (`openai`/`gemini`/`openrouter`/`groq`/`local`/...) |
| `apiKey` | `String` | `""` | API key |
| `baseUrl` | `String` | `""` | Endpoint base (empty = provider default) |
| `model` | `String` | `""` | Default model |
| `models` | `List<String>` | empty | Model list (failover within a provider) |
| `priority` | `int` | `10` | Lower = tried first |
| `enabled` | `boolean` | `true` | Participation in failover |
| `timeoutMs` | `int` | `30000` | HTTP timeout |

### ProviderHealth.java

🔗 [ProviderHealth.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/ai/ProviderHealth.java)

Immutable health snapshot of a provider, used by `/carpetplayers ai status` and cooldown logic.

| Field | Type | Description |
|---|---|---|
| `providerName` | `String` | Provider name |
| `enabled` | `boolean` | Enabled flag |
| `onCooldown` | `boolean` | Currently cooling down |
| `priority` | `int` | Priority score |
| `failureCount` | `int` | Consecutive failures |
| `cooldownUntil` | `long` | Epoch ms when cooldown ends |
| `lastError` | `String` | Last failure message |

---

## Bot Package

### BotBrain.java

🔗 [BotBrain.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/bot/BotBrain.java)

The core per-bot behaviour engine (983 lines). Wraps a Carpet `EntityPlayerMPFake`, implements a state machine (`FOLLOW`, `WANDER`, `PVP`, `CHILL`, `EAT`), the AI tool API (`aiMove`, `aiJump`, `aiSneak`, `aiLookAt`, `aiAttack`, `aiEat`, `aiChat`, `aiMineAt`, `aiUseItem`, `aiDropItem`, `aiRunCommand`, ...), combat AI (targeting, weapon scoring/selection, bow/toss usage), eating/potion/milk logic, wandering with hazard avoidance, chat interaction, and remote "control" mode.

| Member | Type | Description |
|---|---|---|
| `BotState` | enum | `FOLLOW, WANDER, PVP, CHILL, EAT` |
| `bot` | `EntityPlayerMPFake` | The underlying FakePlayer |
| `uuid` / `random` | `UUID` / `Random` | Bot identity / seeded RNG |
| `aiQueue` / `aiQueueActive` | fields | AI tool action queue |
| `aiMove/aiJump/aiSneak/aiLookAt/aiAttack/...` | methods | AI tool API used by `MinecraftToolManager` |
| `aiRunCommand(command)` | method | Executes a server command as the bot |
| `aiGetStateInfo()` | method | Rich state string for the LLM |
| `tick()` | method | Per-tick dispatch to states |
| `combatTick()` | method | PvP targeting/movement/attacks |
| `tickFollow` / `tickWander` / `tickEat` | methods | Behaviour states |
| `weaponScore` / `selectBestWeaponSlot` | methods | Multi-weapon PvP system |
| `tryEat` / `usePotionIfLow` / `tryUseMilk` | methods | Item-usage AI |
| `tickControlled(controller)` | method | Player-driven remote control mode |
| `handleChatCommand(command)` | method | Voice commands (follow/stop/pvp/...) |
| `onAttacked(attacker)` | method | React to damage by entering PvP |

```java
public void tick() {
    if (!bot.isAlive()) return;
    if (ModConfig.instance.useItemEnabled) {
        tryEat();
        usePotionIfLow();
        tryUseMilk();
    }
    tickBowRelease();
    if (ModConfig.instance.interactiveEnabled) tickChat();
    if (!aiQueue.isEmpty()) { tickAiActions(); return; }
    switch (state) {
        case PVP:    combatTick(); break;
        case FOLLOW: tickFollow(); break;
        case WANDER: tickWander(); tickMine(); break;
        case CHILL:  actions().setForward(0.0F); actions().setStrafing(0.0F); break;
        case EAT:    tickEat(); break;
    }
}
```

### BotManager.java

🔗 [BotManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/bot/BotManager.java)

Static registry and lifecycle manager for all bots. Holds concurrent maps of bots, brains, and controlled bots; registers the full `/carpetplayers` command tree (spawn, useitem, interactive, pvp taps, ai, control/release, remove, list, kit); spawns FakePlayers via `EntityPlayerMPFake.createFake`; ticks all brains every server tick; and removes dead bots.

| Member | Type | Description |
|---|---|---|
| `BOTS` | `Map<UUID, EntityPlayerMPFake>` | All spawned bots |
| `BRAINS` | `Map<UUID, BotBrain>` | Bot behaviour objects |
| `CONTROLLED` | `Map<UUID, EntityPlayerMPFake>` | Bot → controlling player |
| `registerCommands(dispatcher, dedicated)` | static | Registers the command tree |
| `spawn` / `spawnPvp` | static | Command handlers for spawning |
| `spawnBots(...)` | private | Core creation via `EntityPlayerMPFake.createFake` |
| `tick(server)` | static | Server-tick handler: ticks brains, prunes dead, drives controls |
| `removeBot(bot)` | static | Removes + kills a bot |
| `nextName(server)` | private | `FriendBot_<n>` unique naming |
| `isDedicated(server)` | static | Singleplayer vs dedicated check |

```java
public static void tick(MinecraftServer server) {
    ModConfig.ensureLoaded();
    if (BOTS.isEmpty() && CONTROLLED.isEmpty()) return;
    List<UUID> dead = new ArrayList<>();
    for (BotBrain brain : BRAINS.values()) {
        if (!brain.getBot().isAlive()) dead.add(brain.getUuid());
        else brain.tick();
    }
    for (UUID uuid : dead) { BOTS.remove(uuid); BRAINS.remove(uuid); }
    for (Map.Entry<UUID, EntityPlayerMPFake> entry : CONTROLLED.entrySet()) {
        ServerPlayer controller = server.getPlayerList().getPlayer(entry.getKey());
        if (controller == null) { CONTROLLED.remove(entry.getKey()); continue; }
        BotBrain brain = BRAINS.get(entry.getValue().getUUID());
        if (brain != null) brain.tickControlled(controller);
    }
}
```

### PvPBot.java

🔗 [PvPBot.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/bot/PvPBot.java)

`BotBrain` subclass specialised for PvP. Starts in `PVP` state, overrides the target radius to the PvP-specific config value, strafes sideways when hurt, and provides a static `equip` helper that outfits a bot with full netherite + sword/bow/golden apple/splash potion/arrows.

| Member | Type | Description |
|---|---|---|
| ctor | method | Sets initial state to `BotState.PVP` |
| `targetRadius()` | override | Uses `ModConfig.instance.pvpTargetRadius` |
| `combatTick()` | override | Strafe-on-hit logic then `super.combatTick()` |
| `equip(bot)` | static | Default netherite + combat inventory |

```java
public class PvPBot extends BotBrain {
    public PvPBot(EntityPlayerMPFake bot) {
        super(bot);
        this.state = BotState.PVP;
    }
    @Override
    protected int targetRadius() { return ModConfig.instance.pvpTargetRadius; }

    @Override
    protected void combatTick() {
        if (ModConfig.instance.useItemEnabled) usePotionIfLow();
        if (bot.hurtTime > 0 && random.nextInt(4) == 0) {
            lastStrafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicks = 10;
        }
        super.combatTick();
        if (strafeTicks > 0) { strafeTicks--; actions().setStrafing(lastStrafeDirection); }
    }
}
```

### KitManager.java

🔗 [KitManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/bot/KitManager.java)

Static utility that equips six PvP kits on a bot: `netherite_crystal`, `diamond_crystal`, `netherite_pot`, `diamond_pot`, `netherite_basic`, `diamond_basic`. Builds enchanted armor/swords, adds totems, and fills inventories with crystals/obsidian/pearls/XP for crystal kits or golden apples/cooked beef/splash healing potions otherwise.

| Member | Type | Description |
|---|---|---|
| `applyKit(bot, kitName)` | static | Dispatch to the correct kit config |
| `equipKit(bot, netherite, crystal, pot)` | private | Clears inventory, applies armor/sword/items |
| `applyArmor(bot, netherite)` | private | Enchanted helmet/chest/legs/boots |
| `applySword(bot, netherite, crystal)` | private | Sharpness V sword (Looting III for crystal) |
| `addSplashHealthPotions(bot, count)` | private | Strong healing splash potions |
| `addItems(bot, item, count)` | private | Stack-aware item adding |
| `enchanted(stack, enchantments)` | private | Applies enchantments via `EnchantmentHelper` |

```java
private static void equipKit(BotBrain bot, boolean netherite, boolean crystal, boolean pot) {
    bot.getBot().inventory.clearContent();
    applyArmor(bot, netherite);
    applySword(bot, netherite, crystal);
    addItems(bot, Items.TOTEM_OF_UNDYING, 3);
    if (crystal) {
        addItems(bot, Items.END_CRYSTAL, 3);
        addItems(bot, Items.OBSIDIAN, 64);
        addItems(bot, Items.ENDER_PEARL, 16);
        addItems(bot, Items.EXPERIENCE_BOTTLE, 8);
    } else {
        addItems(bot, Items.GOLDEN_APPLE, 8);
        addItems(bot, Items.COOKED_BEEF, 64);
        if (pot) addSplashHealthPotions(bot, 16);
    }
}
```

---

## Config Package

### ModConfig.java

🔗 [ModConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/config/ModConfig.java)

Singleton mod configuration persisted to `config/carpetplayers-config.json`. Stores feature toggles (item usage, interactive chat, multi-weapon), tap-hit controls, bot limits, wander/PvP radii, and debug logging. Provides static `ensureLoaded`/`save` plus a `tapControls`/`setTap` helper API.

| Field / Member | Type | Description |
|---|---|---|
| `useItemEnabled` / `interactiveEnabled` / `multiWeaponEnabled` | `boolean` | Feature toggles (default `true`) |
| `tapW/A/S/DEnabled` | `boolean` | Tap-hit controls (default `false`) |
| `maxBots` | `int` | Max concurrent bots (`50`) |
| `wanderRadius` / `pvpTargetRadius` / `baseTargetRadius` | `int` | Behaviour radii |
| `debugLogging` | `boolean` | Debug output (default `true`) |
| `instance` | static | Singleton instance |
| `ensureLoaded()` | static | Load from disk or write defaults |
| `save()` | static | Persist to JSON |
| `tapControls()` | method | Map of tap → enabled |
| `setTap(tap, enabled)` | method | Set a single tap control |

```java
public static void ensureLoaded() {
    if (configFile == null) {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(),
                "carpetplayers-config.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                CarpetPlayersMod.LOGGER.error("Failed to load Carpet Players config", e);
            }
        } else {
            save();
        }
    }
}
```

---

## Mixin Package

### LivingEntityMixin.java

🔗 [LivingEntityMixin.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/mixin/LivingEntityMixin.java)

Mixin on `LivingEntity.hurt`. When a FakePlayer bot is damaged by a real `ServerPlayer`, it notifies the bot's brain (`onAttacked` → switches to PvP) and, if defensive AI is enabled, triggers an asynchronous AI chat reaction via `AIController.runChat`.

| Member | Type | Description |
|---|---|---|
| `@Mixin(LivingEntity.class)` | annotation | Targets vanilla living entities |
| `carpetplayers$onHurt(source, amount, cir)` | inject | `@Inject(method = "hurt", at = @At("HEAD"))` |
| guard | logic | Only acts when `self instanceof EntityPlayerMPFake` and attacker is a `ServerPlayer` |

```java
@Inject(method = "hurt", at = @At("HEAD"))
private void carpetplayers$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    Object self = this;
    if (!(self instanceof EntityPlayerMPFake)) return;
    Entity attacker = source.getEntity();
    if (!(attacker instanceof ServerPlayer)) return;
    BotBrain brain = BotManager.BRAINS.get(((Entity) self).getUUID());
    if (brain != null) {
        brain.onAttacked(attacker);
        if (AIProviderManager.instance().isEnabled()
                && AIProviderManager.instance().isDefensiveEnabled()) {
            AIController.runChat(brain.getBotName(),
                    "You were just attacked by player " + attacker.getName().getString() + ".");
        }
    }
}
```

### ServerGamePacketListenerImplMixin.java

🔗 [ServerGamePacketListenerImplMixin.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/src/main/java/com/carpetplayers/mixin/ServerGamePacketListenerImplMixin.java)

Mixin on `ServerGamePacketListenerImpl.handleChat`. Intercepts player chat: if the sender is controlling a bot, the message is cancelled and broadcast as the bot; otherwise, when interactive mode is on and a message mentions a bot name, it either triggers AI chat (if enabled), a keyword chat command (`follow`/`stop`/`pvp`/`chill`/`wander`/`eat`/`menu`), or a generic canned reply.

| Member | Type | Description |
|---|---|---|
| `@Mixin(ServerGamePacketListenerImpl.class)` | annotation | Targets the server packet handler |
| `player` | `@Shadow` | The sender `ServerPlayer` |
| `carpetplayers$onHandleChat(message, ci)` | inject | `@Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)` |
| `broadcastAsBot(bot, message)` | static | Broadcast `<BotName> message` |
| `extractCommand(lower)` | static | Keyword → command mapping |
| `getReply()` | static | Random canned reply |

```java
@Inject(method = "handleChat(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
private void carpetplayers$onHandleChat(String message, CallbackInfo ci) {
    if (message.startsWith("/")) return;
    ServerPlayer sender = this.player;
    if (sender == null) return;
    EntityPlayerMPFake controlled = BotManager.CONTROLLED.get(sender.getUUID());
    if (controlled != null && controlled.isAlive()) {
        ci.cancel();
        broadcastAsBot(controlled, message);
        return;
    }
    if (ModConfig.instance.interactiveEnabled) {
        String lower = message.toLowerCase();
        String command = extractCommand(lower);
        boolean aiChat = AIProviderManager.instance().isEnabled()
                && AIProviderManager.instance().isChatEnabled();
        for (BotBrain brain : BotManager.BRAINS.values()) {
            if (lower.contains(brain.getBotName().toLowerCase())) {
                if (aiChat) AIController.runChat(brain.getBotName(), message);
                else if (command != null) brain.handleChatCommand(command);
                else brain.pendingReply = getReply();
            }
        }
    }
}
```

---

*End of part 1 — Build & Config, Entry Point, AI, Bot, Config, and Mixin packages.*
# CarpetPlayers — Code Documentation (Paper Plugins)

> Source: [Plugins/](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/tree/main/Plugins)

This document covers the two Paper plugin ports of the Fabric mod **Carpet Players**:

- **Paper 1.16.5 plugin** (`Plugins/`) — obfuscated NMS (`net.minecraft.server.v1_16_R3`), Java 8.
- **Paper 1.21.11 plugin** (`Plugins/1.21.11/`) — Mojang-mapped NMS (`net.minecraft.*`), Java 21, with ViaVersion/ViaBackwards support.

Both plugins share the same architecture: a `BotManager` spawns `FakePlayer` NMS entities driven by a `BotBrain` state machine, and an optional AI layer (`AIController` + provider abstraction) lets an LLM control bots through tool calling.

---

## Paper 1.16.5 Plugin

### Project Structure

```
Plugins/
├── build.gradle
├── settings.gradle
└── src/main/
    ├── resources/plugin.yml
    └── java/com/carpetplayers/
        ├── CarpetPlayersPlugin.java        # JavaPlugin entry point
        ├── config/
        │   └── ModConfig.java              # JSON config (carpetplayers-config.json)
        ├── ai/                             # LLM provider + tool-calling layer
        │   ├── AIController.java
        │   ├── AIProviderManager.java
        │   ├── AIConfig.java
        │   ├── AICommands.java
        │   ├── MinecraftToolManager.java
        │   ├── AITool.java
        │   ├── AIMessage.java
        │   ├── AIToolCall.java
        │   ├── AIResponse.java
        │   ├── AIException.java
        │   ├── AIProvider.java
        │   ├── AbstractAIProvider.java
        │   ├── GeminiProvider.java
        │   ├── OpenAICompatibleProvider.java
        │   ├── ProviderConfig.java
        │   └── ProviderHealth.java
        ├── bot/                            # Bot behaviour
        │   ├── BotBrain.java
        │   ├── BotManager.java
        │   ├── PvPBot.java
        │   └── KitManager.java
        └── nms/                            # Fake player NMS entities
            ├── FakePlayer.java
            ├── FakePlayerConnection.java
            └── FakePlayerFactory.java
```

### Build & Configuration

#### build.gradle

**File:** `Plugins/build.gradle`
**GitHub:** [build.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/build.gradle)

Build script for the 1.16.5 plugin. It compiles against a single extracted Paper server jar (`libs/paper-server-1.16.5.jar`) that bundles paper-api, CraftBukkit and NMS `v1_16_R3`, plus Gson as `compileOnly`. Targets Java 8 with UTF-8 encoding.

| Key | Value |
|---|---|
| `group` / `version` | `com.carpetplayers` / `1.0.0` |
| Dependencies | `compileOnly files('libs/paper-server-1.16.5.jar')`, `compileOnly com.google.code.gson:gson:2.8.9` |
| Java | `sourceCompatibility = VERSION_1_8`, `options.release = 8` |

```gradle
dependencies {
    // Paper 1.16.5 server jar hasil ekstrak paperclip (paper-server-1.16.5.jar):
    // berisi paper-api + CraftBukkit + NMS v1_16_R3. Cukup satu jar untuk kompilasi.
    compileOnly files('libs/paper-server-1.16.5.jar')
    compileOnly 'com.google.code.gson:gson:2.8.9'
}
```

#### settings.gradle

**File:** `Plugins/settings.gradle`
**GitHub:** [settings.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/settings.gradle)

Minimal Gradle settings file naming the root project.

| Key | Value |
|---|---|
| `rootProject.name` | `carpetplayers-plugin` |

```gradle
rootProject.name = 'carpetplayers-plugin'
```

#### plugin.yml

**File:** `Plugins/src/main/resources/plugin.yml`
**GitHub:** [plugin.yml](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/resources/plugin.yml)

Bukkit plugin descriptor. Declares the main class, a single `carpetplayers` command (aliases `cp`, `cpbot`) and the `carpetplayers.admin` permission (default op).

| Key | Value |
|---|---|
| `name` / `version` | `CarpetPlayers` / `1.0.0` |
| `main` | `com.carpetplayers.CarpetPlayersPlugin` |
| `api-version` | `1.16` |
| Commands | `carpetplayers` (aliases: `cp`, `cpbot`) |
| Permissions | `carpetplayers.admin` (default: op) |

```yaml
commands:
  carpetplayers:
    description: Main command for Carpet Players plugin
    usage: /carpetplayers <subcommand>
    permission: carpetplayers.admin
    aliases: [cp, cpbot]
```

### Entry Point & Config

#### CarpetPlayersPlugin.java

**File:** `Plugins/src/main/java/com/carpetplayers/CarpetPlayersPlugin.java`
**GitHub:** [CarpetPlayersPlugin.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/CarpetPlayersPlugin.java)

The `JavaPlugin` entry point. On enable it loads config, registers commands/events and schedules a 20-tick-per-second `BotManager::tick` task (equivalent to Fabric's `ServerTickEvents.END_SERVER_TICK`). On disable it removes all bots and shuts down the AI executor.

| Member | Type | Purpose |
|---|---|---|
| `MOD_ID` | `static final String` | `"carpetplayers"` |
| `instance` | `static CarpetPlayersPlugin` | Global plugin instance |
| `onEnable()` | `void` | Loads config, registers commands/events, starts tick task |
| `onDisable()` | `void` | Removes bots, shuts down AI |
| `log(String)` / `logError(String, Throwable)` | `static void` | Logging helpers |

```java
@Override
public void onEnable() {
    instance = this;
    ModConfig.ensureLoaded();
    AIProviderManager.instance().ensureLoaded();
    BotManager.registerCommands(this);
    BotManager.registerEvents(this);
    // Tick 20x/detik di server thread, setara ServerTickEvents.END_SERVER_TICK.
    getServer().getScheduler().runTaskTimer(this, BotManager::tick, 0L, 1L);
    getLogger().info("Carpet Players plugin loaded!");
}
```

#### ModConfig.java

**File:** `Plugins/src/main/java/com/carpetplayers/config/ModConfig.java`
**GitHub:** [ModConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/config/ModConfig.java)

Main plugin configuration stored as pretty-printed JSON at `plugins/CarpetPlayers/carpetplayers-config.json`, with a file format identical to the Fabric version. Holds behaviour toggles (use-item, interactive, multi-weapon, tap attacks), bot limits and radii.

| Field / Method | Type | Purpose |
|---|---|---|
| `useItemEnabled`, `interactiveEnabled`, `multiWeaponEnabled` | `boolean` | Behaviour toggles (default true) |
| `tapWEnabled` … `tapDEnabled` | `boolean` | Tap-hit controls (default false) |
| `maxBots`, `wanderRadius`, `pvpTargetRadius`, `baseTargetRadius` | `int` | Limits and radii |
| `instance` | `static ModConfig` | Singleton loaded from disk |
| `ensureLoaded()` | `static void` | Loads JSON or saves defaults |
| `save()` | `static void` | Writes JSON to disk |
| `tapControls()` / `setTap(String, boolean)` | — | Tap control map / setter |

```java
public static void ensureLoaded() {
    if (configFile == null) {
        configFile = new File(CarpetPlayersPlugin.instance.getDataFolder(), "carpetplayers-config.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                CarpetPlayersPlugin.logError("Failed to load Carpet Players config", e);
            }
        } else {
            save();
        }
    }
}
```

### AI Package

#### AIController.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIController.java`
**GitHub:** [AIController.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIController.java)

Runs the AI tool-calling loop for a bot on a separate daemon thread so the server thread is never blocked. It iterates up to `MAX_ITERATIONS` (6) rounds, sending messages with tools, executing each returned tool call on the server thread (with a 5-second timeout via `CountDownLatch`), and keeps per-bot chat/act memory (max 12 entries).

| Constant | Value | Purpose |
|---|---|---|
| `MAX_ITERATIONS` | 6 | Max tool-call rounds |
| `TOOL_TIMEOUT_SECONDS` | 5 | Tool execution timeout |
| `MAX_MESSAGES` | 24 | Message window cap |
| `MAX_TOOL_RESULT_CHARS` | 800 | Tool result truncation |
| `MEMORY_MAX` | 12 | Per-bot memory size |

| Method | Purpose |
|---|---|
| `run(botName, instruction, onResult, onError)` | Starts async act loop |
| `runChat(botName, instruction)` | Starts async chat loop |
| `clearMemory(botName)` | Clears bot conversation memory |
| `executeToolOnServer(...)` | Runs a tool on the server thread with timeout |
| `deliverResult(...)` | Schedules callback on the server thread |

```java
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
```

#### AIProviderManager.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIProviderManager.java`
**GitHub:** [AIProviderManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIProviderManager.java)

Singleton manager for AI providers. Loads `minecraft-ai/providers.json` from the plugin data folder, builds provider instances from `ProviderConfig` (Gemini vs OpenAI-compatible), sorts by priority, and dispatches messages with automatic failover across providers/models. Also exposes async send helpers and a provider test routine.

| Member | Type | Purpose |
|---|---|---|
| `instance()` | `static synchronized` | Singleton accessor |
| `ensureLoaded()` / `load()` / `reload()` / `save()` | — | Config lifecycle |
| `defaultProvider(type)` | `ProviderConfig` | Defaults for openai/gemini/openrouter/groq/local |
| `setProviderApiKey(type, apiKey)` | `String` | Adds/updates a provider key |
| `sendMessageInternal(...)` | `AIResponse` | Failover dispatch across providers/models |
| `sendMessageAsync` / `sendMessageWithToolsAsync` | — | Async variants on cached thread pool |
| `testProvidersAsync(onResult)` | — | Connection test for all providers |
| `shutdown()` | `void` | Shuts down the executor |

```java
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
        for (String model : provider.getModels()) {
            try {
                AIResponse response = useTools
                        ? provider.sendMessageWithTools(messages, tools, model)
                        : provider.sendMessage(messages, model);
                provider.markSuccess();
                return response;
            } catch (AIException e) {
                provider.markFailure(e);
            }
        }
    }
    throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
            "No AI provider/model available");
}
```

#### AIConfig.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIConfig.java`
**GitHub:** [AIConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIConfig.java)

Plain data holder for the AI configuration serialized to `providers.json`. Contains global toggles, the system prompt (in Indonesian for 1.16.5), timeouts and the list of `ProviderConfig` entries.

| Field | Type | Default |
|---|---|---|
| `enabled`, `aiChatEnabled`, `aiDefensiveEnabled` | `boolean` | `true` |
| `systemPrompt` | `String` | Indonesian bot-brain prompt |
| `requestTimeoutMs` | `int` | `30000` |
| `failureCooldownMs` | `int` | `30000` |
| `debugLogging` | `boolean` | `false` |
| `providers` | `List<ProviderConfig>` | empty |

```java
public class AIConfig {
    public boolean enabled = true;
    public boolean aiChatEnabled = true;
    public boolean aiDefensiveEnabled = true;
    public String systemPrompt = "Kamu adalah otak dari bot Minecraft (FakePlayer) bernama Carpet Players. "
            + "Kamu mengendalikan gerakan dan aksi bot di dalam game Minecraft 1.16.5. "
            + "Berbicaralah dengan singkat dan gunakan Bahasa Indonesia. "
            + "Kamu dapat menyerang musuh, makan, berjalan, melompat, dan menggunakan item. "
            + "Gunakan tool yang tersedia untuk mengendalikan bot, jangan hanya bercerita.";
    public int requestTimeoutMs = 30000;
    public int failureCooldownMs = 30000;
    public boolean debugLogging = false;
    public List<ProviderConfig> providers = new ArrayList<>();
}
```

#### AICommands.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AICommands.java`
**GitHub:** [AICommands.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AICommands.java)

Static handlers for the `/carpetplayers ai ...` subcommands, invoked by `BotManager`. Each handler sends its result directly to the `CommandSender`.

| Method | Purpose |
|---|---|
| `manager()` | Returns loaded `AIProviderManager` |
| `handleStart` / `handleStop` | Enable/disable AI |
| `handleReload` | Reload provider config |
| `handleStatus` | Print provider status/health |
| `handleTest` | Async connection test |
| `handleAct(sender, botName, instruction)` | Run AI act loop async |
| `handleChat` / `handleForget` / `handleDefensive` | Chat toggle / memory clear / defensive toggle |
| `handleProviderKey(type, apiKey)` | Set provider API key |

```java
public static void handleAct(CommandSender sender, String botName, String instruction) {
    manager();
    sender.sendMessage("[AI] Memproses instruksi untuk bot '" + botName + "'...");
    AIController.run(botName, instruction,
            result -> sender.sendMessage("[AI] " + result),
            error -> sender.sendMessage("[AI] Gagal: " + error));
}
```

#### MinecraftToolManager.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/MinecraftToolManager.java`
**GitHub:** [MinecraftToolManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/MinecraftToolManager.java)

Registry of AI tools exposed to the LLM. Registers 13 tools (`get_state`, `move`, `jump`, `sneak`, `look_at`, `attack`, `eat`, `chat`, `stop`, `set_state`, `mine_block`, `use_item`, `drop_item`, `equip_kit`) as `AITool` instances with JSON-Schema parameters. `executeTool` never throws — failures are returned as strings.

| Member | Type | Purpose |
|---|---|---|
| `instance` | `static final` | Singleton |
| `getTools()` | `List<AITool>` | Unmodifiable tool list |
| `executeTool(name, args, bot)` | `String` | Safe tool dispatch |
| `findBotByName(name)` | `static BotBrain` | Lookup bot by name |

```java
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
```

#### AITool.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AITool.java`
**GitHub:** [AITool.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AITool.java)

Immutable tool definition: name, description, JSON-Schema parameters and an `Executor` lambda that receives parsed args and the `BotBrain`. Static factories build parameter schemas (`stringParam`, `intParam`, `doubleParam`, `booleanParam`, `enumParam`) and assemble them into an object schema.

| Member | Type | Purpose |
|---|---|---|
| `name`, `description`, `parameters` | `final` | Tool metadata |
| `Executor` | `interface` | `String execute(JsonObject, BotBrain)` |
| `from(String)` | `static JsonObject` | Parse raw JSON args safely |
| `objectParams(...)` | `static JsonObject` | Build object JSON Schema |
| `noParams()` | `static JsonObject` | Empty schema |

```java
public static JsonObject objectParams(JsonObject... params) {
    JsonObject result = new JsonObject();
    result.addProperty("type", "object");
    JsonObject properties = new JsonObject();
    JsonArray required = new JsonArray();
    if (params != null) {
        for (JsonObject param : params) {
            if (param == null || !param.has("name") || !param.has("schema")) {
                continue;
            }
            String name = param.get("name").getAsString();
            properties.add(name, param.getAsJsonObject("schema"));
            if (param.has("required") && param.get("required").getAsBoolean()) {
                required.add(name);
            }
        }
    }
    result.add("properties", properties);
    if (required.size() > 0) {
        result.add("required", required);
    }
    return result;
}
```

#### AIMessage.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIMessage.java`
**GitHub:** [AIMessage.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIMessage.java)

Immutable chat message with role, content, optional tool-call id and optional tool calls. Static factories create `system`, `user`, `assistant`, `tool` and `assistantWithTools` messages.

| Field | Type | Purpose |
|---|---|---|
| `role` | `String` | `system` / `user` / `assistant` / `tool` |
| `content` | `String` | Message text |
| `toolCallId` | `String` | Tool result correlation id |
| `toolCalls` | `List<AIToolCall>` | Assistant tool calls |

```java
public static AIMessage tool(String toolCallId, String content) {
    return new AIMessage("tool", content, toolCallId, null);
}

public static AIMessage assistantWithTools(String content, List<AIToolCall> toolCalls) {
    return new AIMessage("assistant", content, null, toolCalls);
}
```

#### AIToolCall.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIToolCall.java`
**GitHub:** [AIToolCall.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIToolCall.java)

Immutable record of a tool invocation requested by the model: id, tool name and raw JSON argument string.

| Field | Type | Purpose |
|---|---|---|
| `id` | `String` | Tool call id |
| `name` | `String` | Tool name |
| `arguments` | `String` | Raw JSON args |

```java
public class AIToolCall {
    public final String id;
    public final String name;
    public final String arguments; // raw JSON string of args

    public AIToolCall(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }
}
```

#### AIResponse.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIResponse.java`
**GitHub:** [AIResponse.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIResponse.java)

Immutable model response: text content, provider/model metadata, raw body and any tool calls.

| Field | Type | Purpose |
|---|---|---|
| `content` | `String` | Reply text |
| `providerName`, `model` | `String` | Source metadata |
| `raw` | `String` | Raw response body |
| `toolCalls` | `List<AIToolCall>` | Tool calls (nullable) |

```java
public AIResponse(String content, String providerName, String model, String raw,
                  List<AIToolCall> toolCalls) {
    this.content = content;
    this.providerName = providerName;
    this.model = model;
    this.raw = raw;
    this.toolCalls = toolCalls;
}
```

#### AIException.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIException.java`
**GitHub:** [AIException.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIException.java)

Checked exception carrying an `ErrorType` enum, provider name, model and HTTP status code for structured error handling across providers.

| Member | Type | Purpose |
|---|---|---|
| `ErrorType` | `enum` | `AUTH`, `RATE_LIMIT`, `QUOTA`, `NETWORK`, `HTTP`, `MODEL_NOT_FOUND`, `NO_PROVIDER`, `UNKNOWN` |
| `type`, `providerName`, `model`, `statusCode` | `final` | Error metadata |

```java
public enum ErrorType {
    AUTH, RATE_LIMIT, QUOTA, NETWORK, HTTP,
    MODEL_NOT_FOUND, NO_PROVIDER, UNKNOWN
}
```

#### AIProvider.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AIProvider.java`
**GitHub:** [AIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AIProvider.java)

Interface implemented by all AI backends. Defines metadata accessors, message sending (with a default no-tools fallback), connection testing, health reporting and success/failure/cooldown tracking.

| Method | Purpose |
|---|---|
| `getName()` / `getType()` / `getPriority()` | Provider metadata |
| `isEnabled()` | Config toggle |
| `getModels()` | Available model list |
| `sendMessage(messages, model)` | Plain chat completion |
| `sendMessageWithTools(messages, tools, model)` | Tool-calling (default: plain) |
| `testConnection()` | Connectivity check |
| `getHealth()` | `ProviderHealth` snapshot |
| `markSuccess()` / `markFailure(AIException)` / `onCooldown()` | Health tracking |

```java
public interface AIProvider {
    String getName();
    String getType();
    boolean isEnabled();
    int getPriority();
    List<String> getModels();
    AIResponse sendMessage(List<AIMessage> messages, String model) throws AIException;

    default AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools,
                                            String model) throws AIException {
        return sendMessage(messages, model);
    }

    boolean testConnection();
    ProviderHealth getHealth();
    void markSuccess();
    void markFailure(AIException exception);
    boolean onCooldown();
}
```

#### AbstractAIProvider.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/AbstractAIProvider.java`
**GitHub:** [AbstractAIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/AbstractAIProvider.java)

Base class implementing shared provider logic: config-backed metadata, model list resolution, health/cooldown tracking, HTTP error classification and a `postJson` helper using `HttpURLConnection`.

| Member | Type | Purpose |
|---|---|---|
| `config` | `protected final ProviderConfig` | Provider settings |
| `failureCount`, `cooldownUntil`, `lastError` | `volatile` | Health state |
| `classifyHttpError(code, body)` | `AIException` | Maps HTTP codes to `ErrorType` |
| `postJson(url, jsonBody, headers)` | `HttpResult` | POST helper with timeouts |
| `HttpResult` | `static class` | `code` + `body` |

```java
protected AIException classifyHttpError(int code, String body) {
    String reason = body != null && !body.isEmpty() ? body : "HTTP " + code;
    switch (code) {
        case 401:
        case 403:
            return new AIException(AIException.ErrorType.AUTH, getName(), null, code,
                    "Authentication failed (" + code + "): " + reason);
        case 429:
            return new AIException(AIException.ErrorType.RATE_LIMIT, getName(), null, code,
                    "Rate limited (" + code + "): " + reason);
        case 402:
            return new AIException(AIException.ErrorType.QUOTA, getName(), null, code,
                    "Quota exceeded (" + code + "): " + reason);
        default:
            return new AIException(AIException.ErrorType.HTTP, getName(), null, code,
                    "Unexpected HTTP error (" + code + "): " + reason);
    }
}
```

#### GeminiProvider.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/GeminiProvider.java`
**GitHub:** [GeminiProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/GeminiProvider.java)

Gemini backend calling `POST /v1beta/models/{model}:generateContent?key=...`. Translates the generic message list into Gemini `contents`/`parts` (system prompt moved to `systemInstruction`, tool results to `functionResponse`) and parses `functionCall` parts back into `AIToolCall`s.

| Method | Purpose |
|---|---|
| `buildEndpoint(model)` | Builds generateContent URL with API key |
| `sendMessage(messages, model)` | Plain chat completion |
| `sendMessageWithTools(messages, tools, model)` | Function-calling variant |
| `buildGenerateContentPayload(...)` | Payload translation |
| `classifyGeminiError(code, body)` | Maps Gemini status strings |
| `testConnection()` | Sends "Say: OK" probe |

```java
private String buildGenerateContentPayload(List<AIMessage> messages, List<AITool> tools) {
    JsonObject payload = new JsonObject();
    JsonArray contents = new JsonArray();
    Map<String, String> toolNames = new HashMap<>();
    boolean systemCollected = false;
    for (AIMessage msg : messages) {
        if (msg.role == null) {
            continue;
        }
        if ("system".equals(msg.role)) {
            if (!systemCollected) {
                JsonObject systemInstruction = new JsonObject();
                JsonArray systemParts = new JsonArray();
                JsonObject systemPart = new JsonObject();
                systemPart.addProperty("text", msg.content != null ? msg.content : "");
                systemParts.add(systemPart);
                systemInstruction.add("parts", systemParts);
                payload.add("systemInstruction", systemInstruction);
                systemCollected = true;
            }
            continue;
        }
        // ... role mapping, functionResponse / functionCall translation ...
    }
    return GSON.toJson(payload);
}
```

#### OpenAICompatibleProvider.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/OpenAICompatibleProvider.java`
**GitHub:** [OpenAICompatibleProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/OpenAICompatibleProvider.java)

Backend for any OpenAI-compatible `/chat/completions` API (OpenAI, OpenRouter, Groq, local Ollama). Builds the standard `messages`/`tools` payload with `Authorization: Bearer` header and parses `choices[0].message` including `tool_calls`.

| Method | Purpose |
|---|---|
| `buildEndpoint()` | Picks default base URL by provider type |
| `doSend(messages, tools, model)` | POST + parse response |
| `buildPayload(...)` | Serializes messages/tools |
| `parseToolCalls(message)` | Extracts `AIToolCall` list |
| `testConnection()` | Sends "Say: OK" probe |

```java
private AIResponse doSend(List<AIMessage> messages, List<AITool> tools, String model) throws AIException {
    String endpoint = buildEndpoint();
    String json = buildPayload(messages, tools, model);

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + getApiKey());

    AbstractAIProvider.HttpResult result = postJson(endpoint, json, headers);
    if (result.code < 200 || result.code >= 300) {
        throw classifyHttpError(result.code, result.body);
    }
    JsonObject root = new JsonParser().parse(result.body).getAsJsonObject();
    JsonObject message = root.getAsJsonArray("choices").get(0)
            .getAsJsonObject().getAsJsonObject("message");
    String content = message.has("content") && !message.get("content").isJsonNull()
            ? message.get("content").getAsString() : "";
    return new AIResponse(content, getName(), model, result.body, parseToolCalls(message));
}
```

#### ProviderConfig.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/ProviderConfig.java`
**GitHub:** [ProviderConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/ProviderConfig.java)

Serializable configuration for a single AI provider entry in `providers.json`.

| Field | Type | Default |
|---|---|---|
| `name` | `String` | — |
| `type` | `String` | `"openai"` |
| `apiKey` | `String` | `""` |
| `baseUrl` | `String` | `""` |
| `model` | `String` | `""` |
| `models` | `List<String>` | empty |
| `priority` | `int` | `10` |
| `enabled` | `boolean` | `true` |
| `timeoutMs` | `int` | `30000` |

```java
public class ProviderConfig {
    public String name;
    public String type = "openai";
    public String apiKey = "";
    public String baseUrl = "";
    public String model = "";
    public List<String> models = new ArrayList<>();
    public int priority = 10;
    public boolean enabled = true;
    public int timeoutMs = 30000;
}
```

#### ProviderHealth.java

**File:** `Plugins/src/main/java/com/carpetplayers/ai/ProviderHealth.java`
**GitHub:** [ProviderHealth.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/ai/ProviderHealth.java)

Immutable snapshot of a provider's health used by `/carpetplayers ai status`.

| Field | Type | Purpose |
|---|---|---|
| `providerName` | `String` | Display name |
| `enabled` | `boolean` | Config enabled |
| `onCooldown` | `boolean` | Currently cooling down |
| `priority` | `int` | Failover priority |
| `failureCount` | `int` | Consecutive failures |
| `cooldownUntil` | `long` | Cooldown end timestamp |
| `lastError` | `String` | Last error message |

```java
public ProviderHealth(String providerName, boolean enabled, boolean onCooldown, int priority,
                      int failureCount, long cooldownUntil, String lastError) {
    this.providerName = providerName;
    this.enabled = enabled;
    this.onCooldown = onCooldown;
    this.priority = priority;
    this.failureCount = failureCount;
    this.cooldownUntil = cooldownUntil;
    this.lastError = lastError;
}
```

### Bot Package

#### BotBrain.java

**File:** `Plugins/src/main/java/com/carpetplayers/bot/BotBrain.java`
**GitHub:** [BotBrain.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/bot/BotBrain.java)

The core behaviour controller for a bot. Implements a `BotState` state machine (`FOLLOW`, `WANDER`, `PVP`, `CHILL`, `EAT`), the AI tool API (`aiMove`, `aiJump`, `aiSneak`, `aiLookAt`, `aiAttack`, `aiEat`, `aiChat`, `aiMineAt`, `aiUseItem`, `aiDropItem`, `aiSelectSlot`, `aiStop`), combat logic (targeting, weapon scoring, bow release, throwables, eating/potions/milk), wandering with hazard avoidance, chat, and player-controlled mirroring via `tickControlled`.

| Member | Type | Purpose |
|---|---|---|
| `BotState` | `enum` | `FOLLOW, WANDER, PVP, CHILL, EAT` |
| `bot`, `uuid`, `random` | `protected final` | Entity, id, RNG |
| `tick()` | `void` | Per-tick state machine dispatch |
| `combatTick()` | `protected void` | PvP targeting/attack logic |
| `tickFollow()` / `tickWander()` / `tickEat()` | `protected void` | State behaviours |
| `manageWeapon()` / `weaponScore(...)` | `protected` | Multi-weapon selection |
| `tryEat()` / `usePotionIfLow()` / `tryUseMilk()` | `protected void` | Item usage |
| `handleChatCommand(command)` | `void` | `!bot <command>` chat control |
| `onAttacked(attacker)` | `void` | Retaliate when hit |
| `tickControlled(controller)` | `void` | Mirror a player's movement |

```java
public void tick() {
    if (!bot.isAlive()) {
        return;
    }
    if (ModConfig.instance.useItemEnabled) {
        tryEat();
        usePotionIfLow();
        tryUseMilk();
    }
    tickBowRelease();
    if (ModConfig.instance.interactiveEnabled) {
        tickChat();
    }
    if (!aiQueue.isEmpty()) {
        tickAiActions();
        return;
    }
    switch (state) {
        case PVP:    combatTick();    break;
        case FOLLOW: tickFollow();    break;
        case WANDER: tickWander(); tickMine(); break;
        case CHILL:  setMovementInput(0.0F, 0.0F); break;
        case EAT:    tickEat();       break;
    }
}
```

#### BotManager.java

**File:** `Plugins/src/main/java/com/carpetplayers/bot/BotManager.java`
**GitHub:** [BotManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/bot/BotManager.java)

Command executor, tab completer and event listener that owns the bot lifecycle. Maintains `BOTS`, `BRAINS` and `CONTROLLED` maps, spawns/removes bots via `FakePlayerFactory`, runs the global tick, and handles `/carpetplayers` subcommands (`spawn`, `pvp`, `ai`, `control`, `release`, `remove`, `list`, `kit`, `useitem`, `interactive`).

| Member | Type | Purpose |
|---|---|---|
| `BOTS`, `BRAINS`, `CONTROLLED` | `static Map<UUID, ...>` | Bot registries |
| `registerCommands` / `registerEvents` | `static void` | Wire-up |
| `onCommand(...)` | `boolean` | Command dispatch |
| `spawnBots(owner, count, pvp)` | `List<String>` | Spawn N bots |
| `removeBot` / `removeAllBots` | `static void` | Cleanup |
| `tick()` | `static void` | Global 20 Hz tick |
| `onPlayerChat(...)` | `@EventHandler` | `!bot <cmd>` chat commands |
| `onBotDamaged(...)` | `@EventHandler` | Retaliation + defensive AI |
| `onTabComplete(...)` | `List<String>` | Tab suggestions |

```java
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
}
```

#### PvPBot.java

**File:** `Plugins/src/main/java/com/carpetplayers/bot/PvPBot.java`
**GitHub:** [PvPBot.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/bot/PvPBot.java)

`BotBrain` subclass specialised for PvP: starts in `PVP` state, uses the larger `pvpTargetRadius`, strafes randomly when hurt, and provides a static `equip` helper that gives the bot full netherite armour plus sword, bow, golden apple, splash potion and arrows.

| Member | Type | Purpose |
|---|---|---|
| `targetRadius()` | `protected int` | Uses `pvpTargetRadius` |
| `combatTick()` | `protected void` | Adds potion use + strafing |
| `equip(FakePlayer)` | `static void` | Default PvP loadout |

```java
@Override
protected void combatTick() {
    if (ModConfig.instance.useItemEnabled) {
        usePotionIfLow();
    }
    if (bot.getHurtTicks() > 0 && random.nextInt(4) == 0) {
        lastStrafeDirection = random.nextBoolean() ? 1 : -1;
        strafeTicks = 10;
    }
    super.combatTick();
    if (strafeTicks > 0) {
        strafeTicks--;
        setMovementInput(0.0F, lastStrafeDirection);
    }
}
```

#### KitManager.java

**File:** `Plugins/src/main/java/com/carpetplayers/bot/KitManager.java`
**GitHub:** [KitManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/bot/KitManager.java)

Applies one of six PvP kits (`netherite_crystal`, `diamond_crystal`, `netherite_pot`, `diamond_pot`, `netherite_basic`, `diamond_basic`) to a bot. Kits combine netherite/diamond armour with enchanted swords, totems, and either crystal-fight supplies (end crystals, obsidian, pearls, XP bottles) or golden apples, food and splash healing potions.

| Method | Purpose |
|---|---|
| `applyKit(bot, kitName)` | Dispatch to kit builder, returns success |
| `equipKit(bot, netherite, crystal, pot)` | Clears inventory, applies kit |
| `applyArmor` / `applySword` | Enchanted equipment |
| `addSplashHealthPotions` | Strong healing splash potions |
| `addItems` / `addItemToInventory` | Stack-aware item insertion |
| `enchanted(stack, enchants)` | Applies enchantments |

```java
public static boolean applyKit(BotBrain bot, String kitName) {
    if (bot == null || kitName == null) {
        return false;
    }
    switch (kitName) {
        case "netherite_crystal":
            equipKit(bot, true, true, false);
            return true;
        case "diamond_crystal":
            equipKit(bot, false, true, false);
            return true;
        case "netherite_pot":
            equipKit(bot, true, false, true);
            return true;
        case "diamond_basic":
            equipKit(bot, false, false, false);
            return true;
        default:
            return false;
    }
}
```

### NMS Package

#### FakePlayer.java

**File:** `Plugins/src/main/java/com/carpetplayers/nms/FakePlayer.java`
**GitHub:** [FakePlayer.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/nms/FakePlayer.java)

NMS `EntityPlayer` subclass representing a fake player. It installs a dummy `NetworkManager`/`FakePlayerConnection`, exposes position/distance helpers, manual movement input (`setMovementInput` + `applyManualMovement` in `tick`), item/equipment accessors, chat broadcasting, and look/rotation helpers used by `BotBrain`.

| Member | Type | Purpose |
|---|---|---|
| `isFake` | `boolean` | Marker flag |
| `setupDummyConnection(server)` | `private void` | Installs fake connection |
| `setMovementInput(f, s, j)` | `void` | Sets manual input |
| `applyManualMovement()` | `private void` | Applies velocity from input |
| `getItemInMainHand()` | `ItemStack` | Main-hand item |
| `lookAt(x, y, z)` | `void` | Face a coordinate |
| `startUsingItem` / `releaseUsingItem` | `void` | Item use control |
| `getServer()` | `MinecraftServer` | Server accessor |

```java
private void applyManualMovement() {
    if (inputForward == 0.0F && inputStrafe == 0.0F) {
        return;
    }
    double rad = Math.toRadians(yaw);
    double forward = inputForward;
    double strafe = inputStrafe;
    double fx = -Math.sin(rad) * forward;
    double fz = Math.cos(rad) * forward;
    double sx = Math.cos(rad) * strafe;
    double sz = Math.sin(rad) * strafe;
    double speed = 0.22D;
    Vec3D mot = getMot();
    setMot((fx + sx) * speed, mot.y, (fz + sz) * speed);
    if (inputJump && onGround) {
        setMot(mot.x, 0.42D, mot.z);
    }
}
```

#### FakePlayerConnection.java

**File:** `Plugins/src/main/java/com/carpetplayers/nms/FakePlayerConnection.java`
**GitHub:** [FakePlayerConnection.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/nms/FakePlayerConnection.java)

`PlayerConnection` subclass that no-ops all packet sends and disconnects, so the server never crashes trying to reach a bot's non-existent client and never removes the bot from the player list.

| Method | Purpose |
|---|---|
| `sendPacket(Packet<?>)` | no-op |
| `a(Packet, GenericFutureListener)` | no-op (listener overload) |
| `disconnect(String)` / `disconnect(IChatBaseComponent)` | no-op |

```java
@Override
public void sendPacket(Packet<?> packet) {
    // no-op: bot tidak punya klien sungguhan
}

@Override
public void disconnect(String reason) {
    // no-op: jangan biarkan server mengeluarkan bot dari daftar player
}
```

#### FakePlayerFactory.java

**File:** `Plugins/src/main/java/com/carpetplayers/nms/FakePlayerFactory.java`
**GitHub:** [FakePlayerFactory.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/src/main/java/com/carpetplayers/nms/FakePlayerFactory.java)

Factory that spawns and despawns `FakePlayer` instances. Spawn registers the bot into the server `PlayerList` and `WorldServer` and broadcasts `ADD_PLAYER` + `NamedEntitySpawn` packets; despawn broadcasts `EntityDestroy` + `REMOVE_PLAYER` and removes the entity.

| Method | Purpose |
|---|---|
| `spawn(name, world, x, y, z, yaw, pitch)` | Create + register + broadcast |
| `despawn(FakePlayer)` | Broadcast removal + unregister |
| `despawnAll()` | Clean up all worlds/player list |
| `broadcastPacket(Packet)` | Send to all online players |

```java
public static FakePlayer spawn(String name, World bukkitWorld,
                               double x, double y, double z, float yaw, float pitch) {
    CraftServer craftServer = (CraftServer) Bukkit.getServer();
    MinecraftServer server = craftServer.getServer();
    WorldServer world = ((CraftWorld) bukkitWorld).getHandle();
    GameProfile profile = new GameProfile(UUID.randomUUID(), name);
    PlayerInteractManager interactManager = new PlayerInteractManager(world);
    FakePlayer fake = new FakePlayer(server, world, profile, interactManager);
    interactManager.player = fake;
    fake.moveLocation(x, y, z, yaw, pitch);
    fake.isFake = true;

    server.getPlayerList().players.add(fake);
    world.players.add(fake);
    world.addEntity(fake);

    broadcastPacket(new PacketPlayOutPlayerInfo(
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fake));
    broadcastPacket(new PacketPlayOutNamedEntitySpawn(fake));
    return fake;
}
```

---

## Paper 1.21.11 Plugin

### Project Structure

```
Plugins/1.21.11/
├── build.gradle
├── settings.gradle
├── API-REFERENCE.md
└── src/main/
    ├── resources/plugin.yml
    └── java/com/carpetplayers/
        ├── CarpetPlayersPlugin.java        # JavaPlugin entry point
        ├── config/
        │   └── ModConfig.java              # JSON config (carpetplayers-config.json)
        ├── ai/                             # LLM provider + tool-calling layer
        │   ├── AIController.java
        │   ├── AIProviderManager.java
        │   ├── AIConfig.java
        │   ├── AICommands.java
        │   ├── MinecraftToolManager.java
        │   ├── AITool.java
        │   ├── AIMessage.java
        │   ├── AIToolCall.java
        │   ├── AIResponse.java
        │   ├── AIException.java
        │   ├── AIProvider.java
        │   ├── AbstractAIProvider.java
        │   ├── GeminiProvider.java
        │   ├── OpenAICompatibleProvider.java
        │   ├── ProviderConfig.java
        │   └── ProviderHealth.java
        ├── bot/                            # Bot behaviour
        │   ├── BotBrain.java
        │   ├── BotManager.java
        │   ├── PvPBot.java
        │   └── KitManager.java
        ├── nms/                            # Fake player NMS entities
        │   ├── FakePlayer.java
        │   ├── FakePlayerConnection.java
        │   └── FakePlayerFactory.java
        └── via/                            # ViaVersion integration
            └── ViaCompat.java
```

### Build & Configuration

#### build.gradle

**File:** `Plugins/1.21.11/build.gradle`
**GitHub:** [build.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/build.gradle)

Build script for the 1.21.11 plugin. Uses a Java 21 toolchain and compiles against `paper-api-1.21.11.jar`, `paper-server-1.21.11.jar` (Mojang-mapped NMS), the ViaVersion API jar, and the `patchwork/libraries` file tree (Paper 1.21.x separates runtime libraries from the server jar). `processResources` expands `${version}` into `plugin.yml`.

| Key | Value |
|---|---|
| `group` / `version` | `com.carpetplayers` / `1.21.11-1.0.0` |
| Toolchain | Java 21 |
| Dependencies | `compileOnly` paper-api, paper-server, ViaVersion-5.11.0, `patchwork/libraries` |
| `options.release` | `21` |
| Jar name | `carpetplayers-1.21.11.jar` |

```gradle
dependencies {
    compileOnly files('libs/paper-api-1.21.11.jar')
    compileOnly files('libs/paper-server-1.21.11.jar')
    // ViaVersion API - detects the client protocol version (Stage 2)
    compileOnly files('libs/ViaVersion-5.11.0.jar')
    // Paper 1.21.x separates runtime libraries from the server jar (no fat jar).
    compileOnly fileTree(dir: 'patchwork/libraries', include: '**/*.jar')
}
```

#### settings.gradle

**File:** `Plugins/1.21.11/settings.gradle`
**GitHub:** [settings.gradle](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/settings.gradle)

Gradle settings with plugin management repositories and the root project name.

| Key | Value |
|---|---|
| `pluginManagement.repositories` | `gradlePluginPortal()`, `mavenCentral()` |
| `rootProject.name` | `carpetplayers-12111` |

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = 'carpetplayers-12111'
```

#### plugin.yml

**File:** `Plugins/1.21.11/src/main/resources/plugin.yml`
**GitHub:** [plugin.yml](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/resources/plugin.yml)

Bukkit descriptor for the 1.21.11 plugin. Declares `softdepend` on ViaVersion/ViaBackwards/WorldEdit, the `carpetplayers` command (aliases `cp`, `carpet`) and two permissions (`carpetplayers.admin`, `carpetplayers.ai`). The version is expanded from Gradle at build time.

| Key | Value |
|---|---|
| `name` / `version` | `CarpetPlayers` / `${version}` (expanded) |
| `main` | `com.carpetplayers.CarpetPlayersPlugin` |
| `softdepend` | `[ViaVersion, ViaBackwards, WorldEdit]` |
| Commands | `carpetplayers` (aliases: `cp`, `carpet`) |
| Permissions | `carpetplayers.admin`, `carpetplayers.ai` (default: op) |

```yaml
softdepend: [ViaVersion, ViaBackwards, WorldEdit]

commands:
  carpetplayers:
    description: Main command for CarpetPlayers
    usage: /carpetplayers <spawn|pvp|ai|control|release|remove|list|kit|useitem|interactive>
    aliases: [cp, carpet]
    permission: carpetplayers.admin
```

#### API-REFERENCE.md

**File:** `Plugins/1.21.11/API-REFERENCE.md`
**GitHub:** [API-REFERENCE.md](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/API-REFERENCE.md)

Developer reference for the Mojang-mapped NMS API in Paper 1.21.11 (build 130). Documents verified `javap` signatures for `ServerPlayer`, `ServerGamePacketListenerImpl`, `CommonListenerCookie`, `ClientInformation`, `MinecraftServer`, `PlayerList`, `ServerLevel`, `Entity`/`LivingEntity`/`Player`, `ItemStack`, the game packets, `DamageSource`, `BlockPos`, `GameProfile`, `Connection` and Bukkit equivalents — the key differences from the obfuscated 1.16.5 API.

| Section | Content |
|---|---|
| `ServerPlayer` | Constructor, `connection` field, `hurtServer`, `drop`, `tick` |
| `ServerGamePacketListenerImpl` | Constructor, `send`, `disconnect` |
| `PlayerList` | `placeNewPlayer`, `remove`, `getPlayer`, `broadcastAll` |
| `ServerLevel` / `Level` | `addFreshEntity`, `getBlockState`, `getEntitiesOfClass` |
| Packets | `ClientboundPlayerInfoUpdatePacket`, `ClientboundAddPlayerPacket`, etc. |
| Bukkit equivalents | Scoreboard, inventory, health, command dispatch |

```text
### ServerPlayer (net.minecraft.server.level.ServerPlayer)
- Constructor: `ServerPlayer(MinecraftServer, ServerLevel, GameProfile, ClientInformation)`
- `getBukkitEntity()` -> CraftPlayer
- public field: `ServerGamePacketListenerImpl connection`
- `hurtServer(ServerLevel, DamageSource, float)` -> boolean (NOTE: not hurt())
- `drop(ItemStack, boolean, boolean, boolean, Consumer<Item>)` -> ItemEntity
```

### Entry Point & Config

#### CarpetPlayersPlugin.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/CarpetPlayersPlugin.java`
**GitHub:** [CarpetPlayersPlugin.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/CarpetPlayersPlugin.java)

Identical in structure to the 1.16.5 entry point: loads config and AI providers, registers commands/events, and schedules the 20 Hz `BotManager::tick` task. On disable it removes all bots and shuts down the AI executor.

| Member | Type | Purpose |
|---|---|---|
| `MOD_ID` | `static final String` | `"carpetplayers"` |
| `instance` | `static CarpetPlayersPlugin` | Global plugin instance |
| `onEnable()` | `void` | Startup wiring |
| `onDisable()` | `void` | Cleanup |
| `log` / `logError` | `static void` | Logging helpers |

```java
@Override
public void onEnable() {
    instance = this;
    ModConfig.ensureLoaded();
    AIProviderManager.instance().ensureLoaded();
    BotManager.registerCommands(this);
    BotManager.registerEvents(this);
    // Tick 20x/second on the server thread, equivalent to ServerTickEvents.END_SERVER_TICK.
    getServer().getScheduler().runTaskTimer(this, BotManager::tick, 0L, 1L);
    getLogger().info("Carpet Players plugin loaded!");
}
```

#### ModConfig.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/config/ModConfig.java`
**GitHub:** [ModConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/config/ModConfig.java)

Same JSON config as the 1.16.5 version (identical file format to the Fabric mod), stored at `plugins/CarpetPlayers/carpetplayers-config.json`.

| Field / Method | Type | Purpose |
|---|---|---|
| `useItemEnabled`, `interactiveEnabled`, `multiWeaponEnabled` | `boolean` | Behaviour toggles |
| `tapWEnabled` … `tapDEnabled` | `boolean` | Tap-hit controls |
| `maxBots`, `wanderRadius`, `pvpTargetRadius`, `baseTargetRadius` | `int` | Limits and radii |
| `ensureLoaded()` / `save()` | `static` | Load/save JSON |
| `tapControls()` / `setTap(String, boolean)` | — | Tap control helpers |

```java
public static void ensureLoaded() {
    if (configFile == null) {
        configFile = new File(CarpetPlayersPlugin.instance.getDataFolder(), "carpetplayers-config.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                CarpetPlayersPlugin.logError("Failed to load Carpet Players config", e);
            }
        } else {
            save();
        }
    }
}
```

### AI Package

> The 1.21.11 AI package is functionally identical to the 1.16.5 version. The main differences are: English system prompt and messages, `serverOnline` boolean instead of a `MinecraftServer` reference in `AIController`, and `bot.getName().getString()` (Component) in `MinecraftToolManager.findBotByName`.

#### AIController.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIController.java`
**GitHub:** [AIController.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIController.java)

Runs the AI tool-calling loop on a daemon thread with the same constants as 1.16.5 (`MAX_ITERATIONS = 6`, `TOOL_TIMEOUT_SECONDS = 5`, `MAX_MESSAGES = 24`, `MEMORY_MAX = 12`). Tool execution is marshalled to the server thread with a `CountDownLatch` timeout; results and replies are truncated.

| Constant | Value |
|---|---|
| `MAX_ITERATIONS` | 6 |
| `TOOL_TIMEOUT_SECONDS` | 5 |
| `MAX_MESSAGES` | 24 |
| `MAX_TOOL_RESULT_CHARS` | 800 |
| `MEMORY_MAX` | 12 |
| `MAX_REPLY_CHARS` | 500 |

| Method | Purpose |
|---|---|
| `run(botName, instruction, onResult, onError)` | Async act loop |
| `runChat(botName, instruction)` | Async chat loop |
| `clearMemory(botName)` | Clear per-bot memory |
| `executeToolOnServer(serverOnline, bot, toolCall)` | Server-thread tool execution |
| `deliverResult(serverOnline, callback, message)` | Server-thread callback |

```java
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
```

#### AIProviderManager.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIProviderManager.java`
**GitHub:** [AIProviderManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIProviderManager.java)

Identical singleton provider manager to 1.16.5: loads `minecraft-ai/providers.json`, builds providers by type, sorts by priority, and dispatches with failover across providers/models. Provides async send helpers and provider testing.

| Member | Type | Purpose |
|---|---|---|
| `instance()` | `static synchronized` | Singleton |
| `ensureLoaded()` / `load()` / `reload()` / `save()` | — | Config lifecycle |
| `defaultProvider(type)` | `ProviderConfig` | Known provider defaults |
| `setProviderApiKey(type, apiKey)` | `String` | Add/update provider |
| `sendMessageInternal(...)` | `AIResponse` | Failover dispatch |
| `sendMessageAsync` / `sendMessageWithToolsAsync` | — | Async variants |
| `testProvidersAsync(onResult)` | — | Connection tests |
| `shutdown()` | `void` | Executor shutdown |

```java
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
        for (String model : provider.getModels()) {
            try {
                AIResponse response = useTools
                        ? provider.sendMessageWithTools(messages, tools, model)
                        : provider.sendMessage(messages, model);
                provider.markSuccess();
                return response;
            } catch (AIException e) {
                provider.markFailure(e);
            }
        }
    }
    throw new AIException(AIException.ErrorType.NO_PROVIDER, "none", null, 0,
            "No AI provider/model available");
}
```

#### AIConfig.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIConfig.java`
**GitHub:** [AIConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIConfig.java)

Same data holder as 1.16.5 but with an **English** system prompt.

| Field | Type | Default |
|---|---|---|
| `enabled`, `aiChatEnabled`, `aiDefensiveEnabled` | `boolean` | `true` |
| `systemPrompt` | `String` | English bot-brain prompt |
| `requestTimeoutMs` / `failureCooldownMs` | `int` | `30000` |
| `debugLogging` | `boolean` | `false` |
| `providers` | `List<ProviderConfig>` | empty |

```java
public String systemPrompt = "You are the brain of a Minecraft bot (FakePlayer) named Carpet Players. "
        + "You control the bot's movement and actions inside the Minecraft 1.16.5 game. "
        + "Speak briefly and use English. "
        + "You can attack enemies, eat, walk, jump, and use items. "
        + "Use the available tools to control the bot, do not just tell stories.";
```

#### AICommands.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AICommands.java`
**GitHub:** [AICommands.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AICommands.java)

Static handlers for `/carpetplayers ai ...` subcommands — same set as 1.16.5 (`start`, `stop`, `reload`, `status`, `test`, `act`, `chat`, `forget`, `defensive`, `provider`).

| Method | Purpose |
|---|---|
| `manager()` | Loaded `AIProviderManager` |
| `handleStart` / `handleStop` | Enable/disable AI |
| `handleReload` | Reload config |
| `handleStatus` | Provider status output |
| `handleTest` | Async connection test |
| `handleAct(sender, botName, instruction)` | Async act loop |
| `handleChat` / `handleForget` / `handleDefensive` | Toggles / memory clear |
| `handleProviderKey(type, apiKey)` | Set API key |

```java
public static void handleAct(CommandSender sender, String botName, String instruction) {
    manager();
    sender.sendMessage("[AI] Memproses instruksi untuk bot '" + botName + "'...");
    AIController.run(botName, instruction,
            result -> sender.sendMessage("[AI] " + result),
            error -> sender.sendMessage("[AI] Gagal: " + error));
}
```

#### MinecraftToolManager.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/MinecraftToolManager.java`
**GitHub:** [MinecraftToolManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/MinecraftToolManager.java)

Same 13-tool registry as 1.16.5 with English descriptions. The only API difference is `findBotByName`, which compares against `bot.getName().getString()` because `Entity.getName()` returns a `Component` in Mojang-mapped 1.21.11.

| Member | Type | Purpose |
|---|---|---|
| `instance` | `static final` | Singleton |
| `getTools()` | `List<AITool>` | Tool list |
| `executeTool(name, args, bot)` | `String` | Safe dispatch |
| `findBotByName(name)` | `static BotBrain` | Name lookup (Component-aware) |

```java
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
```

#### AITool.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AITool.java`
**GitHub:** [AITool.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AITool.java)

Identical tool definition and JSON-Schema builder to 1.16.5.

| Member | Type | Purpose |
|---|---|---|
| `name`, `description`, `parameters` | `final` | Tool metadata |
| `Executor` | `interface` | Tool execution lambda |
| `from(String)` | `static JsonObject` | Safe JSON args parse |
| `objectParams` / `noParams` | `static JsonObject` | Schema builders |
| `stringParam` / `intParam` / `doubleParam` / `booleanParam` / `enumParam` | `static JsonObject` | Parameter factories |

```java
public static JsonObject from(String json) {
    if (json == null || json.trim().isEmpty()) {
        return new JsonObject();
    }
    try {
        return new JsonParser().parse(json).getAsJsonObject();
    } catch (Exception e) {
        return new JsonObject();
    }
}
```

#### AIMessage.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIMessage.java`
**GitHub:** [AIMessage.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIMessage.java)

Identical immutable message model with static factories.

| Field | Type | Purpose |
|---|---|---|
| `role` | `String` | Message role |
| `content` | `String` | Text |
| `toolCallId` | `String` | Tool result id |
| `toolCalls` | `List<AIToolCall>` | Assistant tool calls |

```java
public static AIMessage system(String content) {
    return new AIMessage("system", content);
}

public static AIMessage user(String content) {
    return new AIMessage("user", content);
}

public static AIMessage assistant(String content) {
    return new AIMessage("assistant", content);
}
```

#### AIToolCall.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIToolCall.java`
**GitHub:** [AIToolCall.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIToolCall.java)

Identical immutable tool-call record.

| Field | Type | Purpose |
|---|---|---|
| `id` | `String` | Call id |
| `name` | `String` | Tool name |
| `arguments` | `String` | Raw JSON args |

```java
public AIToolCall(String id, String name, String arguments) {
    this.id = id;
    this.name = name;
    this.arguments = arguments;
}
```

#### AIResponse.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIResponse.java`
**GitHub:** [AIResponse.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIResponse.java)

Identical immutable response model.

| Field | Type | Purpose |
|---|---|---|
| `content` | `String` | Reply text |
| `providerName`, `model` | `String` | Metadata |
| `raw` | `String` | Raw body |
| `toolCalls` | `List<AIToolCall>` | Tool calls |

```java
public AIResponse(String content, String providerName, String model, String raw,
                  List<AIToolCall> toolCalls) {
    this.content = content;
    this.providerName = providerName;
    this.model = model;
    this.raw = raw;
    this.toolCalls = toolCalls;
}
```

#### AIException.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIException.java`
**GitHub:** [AIException.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIException.java)

Identical checked exception with `ErrorType` enum.

| Member | Type | Purpose |
|---|---|---|
| `ErrorType` | `enum` | `AUTH`, `RATE_LIMIT`, `QUOTA`, `NETWORK`, `HTTP`, `MODEL_NOT_FOUND`, `NO_PROVIDER`, `UNKNOWN` |
| `type`, `providerName`, `model`, `statusCode` | `final` | Error metadata |

```java
public AIException(ErrorType type, String providerName, String model, int statusCode, String message) {
    super(message);
    this.type = type;
    this.providerName = providerName;
    this.model = model;
    this.statusCode = statusCode;
}
```

#### AIProvider.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIProvider.java`
**GitHub:** [AIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AIProvider.java)

Identical provider interface.

| Method | Purpose |
|---|---|
| `getName()` / `getType()` / `getPriority()` | Metadata |
| `isEnabled()` | Config toggle |
| `getModels()` | Model list |
| `sendMessage(messages, model)` | Plain completion |
| `sendMessageWithTools(messages, tools, model)` | Tool-calling (default plain) |
| `testConnection()` | Probe |
| `getHealth()` | Health snapshot |
| `markSuccess()` / `markFailure()` / `onCooldown()` | Health tracking |

```java
default AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools,
                                        String model) throws AIException {
    return sendMessage(messages, model);
}
```

#### AbstractAIProvider.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AbstractAIProvider.java`
**GitHub:** [AbstractAIProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/AbstractAIProvider.java)

Identical base class: config-backed metadata, health/cooldown tracking, HTTP error classification and `postJson` helper.

| Member | Type | Purpose |
|---|---|---|
| `config` | `protected final ProviderConfig` | Settings |
| `failureCount`, `cooldownUntil`, `lastError` | `volatile` | Health state |
| `classifyHttpError(code, body)` | `AIException` | HTTP → `ErrorType` |
| `postJson(url, jsonBody, headers)` | `HttpResult` | POST helper |
| `HttpResult` | `static class` | `code` + `body` |

```java
@Override
public void markFailure(AIException exception) {
    failureCount++;
    cooldownUntil = System.currentTimeMillis() + 30000L;
    lastError = exception.getMessage();
}

@Override
public boolean onCooldown() {
    return System.currentTimeMillis() < cooldownUntil;
}
```

#### GeminiProvider.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/GeminiProvider.java`
**GitHub:** [GeminiProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/GeminiProvider.java)

Identical Gemini backend (generateContent + function calling). The only text difference is the English probe message `"Say: OK"`.

| Method | Purpose |
|---|---|
| `buildEndpoint(model)` | generateContent URL |
| `sendMessage(messages, model)` | Plain completion |
| `sendMessageWithTools(messages, tools, model)` | Function calling |
| `buildGenerateContentPayload(...)` | Payload translation |
| `classifyGeminiError(code, body)` | Status mapping |
| `testConnection()` | Probe |

```java
@Override
public boolean testConnection() {
    try {
        List<AIMessage> messages = new ArrayList<>();
        messages.add(new AIMessage("user", "Say: OK"));
        AIResponse response = sendMessage(messages, getModels().isEmpty() ? "" : getModels().get(0));
        return response != null && response.content != null;
    } catch (AIException e) {
        lastError = e.getMessage();
        return false;
    }
}
```

#### OpenAICompatibleProvider.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/OpenAICompatibleProvider.java`
**GitHub:** [OpenAICompatibleProvider.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/OpenAICompatibleProvider.java)

Identical OpenAI-compatible backend (`/chat/completions` with tool calls). Probe message is English.

| Method | Purpose |
|---|---|
| `buildEndpoint()` | Base URL by provider type |
| `doSend(messages, tools, model)` | POST + parse |
| `buildPayload(...)` | Payload serialization |
| `parseToolCalls(message)` | Tool call extraction |
| `testConnection()` | Probe |

```java
private List<AIToolCall> parseToolCalls(JsonObject message) {
    if (message == null || !message.has("tool_calls") || message.get("tool_calls").isJsonNull()) {
        return null;
    }
    JsonArray calls = message.getAsJsonArray("tool_calls");
    if (calls == null || calls.size() == 0) {
        return null;
    }
    List<AIToolCall> result = new ArrayList<>();
    for (JsonElement el : calls) {
        JsonObject call = el.getAsJsonObject();
        String id = call.has("id") && !call.get("id").isJsonNull()
                ? call.get("id").getAsString() : "";
        JsonObject function = call.has("function") ? call.getAsJsonObject("function") : null;
        String name = function != null && function.has("name")
                ? function.get("name").getAsString() : "";
        String arguments = function != null && function.has("arguments")
                && !function.get("arguments").isJsonNull()
                ? function.get("arguments").getAsString() : "{}";
        result.add(new AIToolCall(id, name, arguments));
    }
    return result;
}
```

#### ProviderConfig.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/ProviderConfig.java`
**GitHub:** [ProviderConfig.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/ProviderConfig.java)

Identical serializable provider config.

| Field | Type | Default |
|---|---|---|
| `name` | `String` | — |
| `type` | `String` | `"openai"` |
| `apiKey` | `String` | `""` |
| `baseUrl` | `String` | `""` |
| `model` | `String` | `""` |
| `models` | `List<String>` | empty |
| `priority` | `int` | `10` |
| `enabled` | `boolean` | `true` |
| `timeoutMs` | `int` | `30000` |

```java
public class ProviderConfig {
    public String name;
    public String type = "openai";
    public String apiKey = "";
    public String baseUrl = "";
    public String model = "";
    public List<String> models = new ArrayList<>();
    public int priority = 10;
    public boolean enabled = true;
    public int timeoutMs = 30000;
}
```

#### ProviderHealth.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/ai/ProviderHealth.java`
**GitHub:** [ProviderHealth.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/ai/ProviderHealth.java)

Identical immutable health snapshot.

| Field | Type | Purpose |
|---|---|---|
| `providerName` | `String` | Display name |
| `enabled` | `boolean` | Config enabled |
| `onCooldown` | `boolean` | Cooldown state |
| `priority` | `int` | Failover priority |
| `failureCount` | `int` | Consecutive failures |
| `cooldownUntil` | `long` | Cooldown end |
| `lastError` | `String` | Last error |

```java
public ProviderHealth(String providerName, boolean enabled, boolean onCooldown, int priority,
                      int failureCount, long cooldownUntil, String lastError) {
    this.providerName = providerName;
    this.enabled = enabled;
    this.onCooldown = onCooldown;
    this.priority = priority;
    this.failureCount = failureCount;
    this.cooldownUntil = cooldownUntil;
    this.lastError = lastError;
}
```

### Bot Package

#### BotBrain.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/bot/BotBrain.java`
**GitHub:** [BotBrain.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/bot/BotBrain.java)

The 1.21.11 behaviour controller, ported to Mojang-mapped NMS. Same `BotState` machine and AI tool API, but with these API changes: `bot.level()` instead of `getWorldServer()`, `getInventory().getItem(i)` instead of `inventory.getItem(i)`, `getSelectedSlot()`/`setSelectedSlot()` instead of `inventory.itemInHandIndex`, `gameMode().useItem(...)`/`gameMode().destroyBlock(pos)` instead of `playerInteractManager`, `getDeltaMovement()`/`setDeltaMovement()` for velocity, and `broadcastSystemMessage` for chat. It also adds `aiRunCommand(command)` which lets the AI execute server commands (with a `/help` special case that lists known commands).

| Member | Type | Purpose |
|---|---|---|
| `BotState` | `enum` | `FOLLOW, WANDER, PVP, CHILL, EAT` |
| `tick()` | `void` | State machine dispatch |
| `aiRunCommand(command)` | `String` | Execute command as bot (new) |
| `combatTick()` / `tickFollow()` / `tickWander()` / `tickEat()` | `protected void` | State behaviours |
| `manageWeapon()` / `weaponScore(...)` | `protected` | Weapon selection |
| `tryEat()` / `usePotionIfLow()` / `tryUseMilk()` | `protected void` | Item usage |
| `gameMode()` / `useItemMainHand()` / `breakBlockAt(pos)` | `protected` | NMS access helpers |
| `setSelectedSlot(int)` / `getSelectedSlot()` | `protected` | Hotbar slot access |
| `handleChatCommand(command)` | `void` | Chat control |
| `onAttacked(attacker)` | `void` | Retaliation |
| `tickControlled(controller)` | `void` | Player mirroring |

```java
public String aiRunCommand(String command) {
    if (command == null || command.trim().isEmpty()) {
        return "Empty command";
    }
    String cmd = command.trim();
    if (cmd.startsWith("/")) {
        cmd = cmd.substring(1);
    }
    String lower = cmd.toLowerCase(Locale.ROOT);
    if (lower.equals("help") || lower.startsWith("help ")) {
        StringBuilder sb = new StringBuilder("Available commands: ");
        for (String name : Bukkit.getCommandMap().getKnownCommands().keySet()) {
            if (name != null && !name.isEmpty() && !name.contains(":")) {
                sb.append('/').append(name).append(' ');
            }
        }
        return sb.toString();
    }
    try {
        boolean ok = bot.getBukkitPlayer().performCommand(cmd);
        return ok ? "Command executed: /" + cmd : "Failed to execute command: /" + cmd;
    } catch (Exception e) {
        return "Error executing command /" + cmd + ": "
                + (e.getMessage() != null ? e.getMessage() : e.toString());
    }
}
```

#### BotManager.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/bot/BotManager.java`
**GitHub:** [BotManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/bot/BotManager.java)

Command executor / tab completer / listener for the 1.21.11 plugin. Same bot lifecycle as 1.16.5 plus a new `protocol` subcommand that reports a player's client protocol version via `ViaCompat` (detecting legacy clients through ViaBackwards). Uses `bot.getName().getString()` for names and `CraftPlayer` from `org.bukkit.craftbukkit.entity`.

| Member | Type | Purpose |
|---|---|---|
| `BOTS`, `BRAINS`, `CONTROLLED` | `static Map<UUID, ...>` | Bot registries |
| `onCommand(...)` | `boolean` | Command dispatch (incl. `protocol`) |
| `cmdProtocol(sender, args)` | `boolean` | ViaVersion protocol report |
| `spawnBots(owner, count, pvp)` | `List<String>` | Spawn bots |
| `removeBot` / `removeAllBots` | `static void` | Cleanup |
| `tick()` | `static void` | Global tick |
| `onPlayerChat(...)` / `onBotDamaged(...)` | `@EventHandler` | Chat commands / retaliation |
| `onTabComplete(...)` | `List<String>` | Suggestions |

```java
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
```

#### PvPBot.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/bot/PvPBot.java`
**GitHub:** [PvPBot.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/bot/PvPBot.java)

PvP-specialised `BotBrain` using `EquipmentSlot` (Mojang-mapped) and `getInventory().setItem(...)`/`setSelectedSlot(...)` for the default netherite loadout.

| Member | Type | Purpose |
|---|---|---|
| `targetRadius()` | `protected int` | `pvpTargetRadius` |
| `combatTick()` | `protected void` | Potions + strafing |
| `equip(FakePlayer)` | `static void` | Default PvP loadout |

```java
public static void equip(FakePlayer bot) {
    bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
    bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
    bot.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
    bot.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
    bot.getInventory().setItem(0, new ItemStack(Items.NETHERITE_SWORD));
    bot.getInventory().setItem(1, new ItemStack(Items.BOW));
    bot.getInventory().setItem(2, new ItemStack(Items.GOLDEN_APPLE));
    bot.getInventory().setItem(3, new ItemStack(Items.SPLASH_POTION));
    bot.getInventory().setItem(4, new ItemStack(Items.ARROW, 64));
    bot.getInventory().setSelectedSlot(0);
}
```

#### KitManager.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/bot/KitManager.java`
**GitHub:** [KitManager.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/bot/KitManager.java)

Kit application for 1.21.11. Because enchantments and potions are now DataComponents, it applies them through the **Bukkit API** (`CraftItemStack.asBukkitCopy` → `ItemMeta.addEnchant` → `asNMSCopy`, and `PotionMeta.setBasePotionType(PotionType.STRONG_HEALING)`), which is the most version-proof approach.

| Method | Purpose |
|---|---|
| `applyKit(bot, kitName)` | Kit dispatch |
| `equipKit(bot, netherite, crystal, pot)` | Inventory clear + kit |
| `applyArmor` / `applySword` | Enchanted equipment |
| `addSplashHealthPotions` | Bukkit-API potions |
| `addItems` / `addItemToInventory` | Stack-aware insertion |
| `enchanted(stack, enchants)` | Bukkit-API enchanting |

```java
private static ItemStack enchanted(ItemStack stack, Map<Enchantment, Integer> enchantments) {
    if (enchantments != null && !enchantments.isEmpty()) {
        org.bukkit.inventory.ItemStack bukkit = CraftItemStack.asBukkitCopy(stack);
        ItemMeta meta = bukkit.getItemMeta();
        if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            bukkit.setItemMeta(meta);
        }
        return CraftItemStack.asNMSCopy(bukkit);
    }
    return stack;
}
```

### NMS Package

#### FakePlayer.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayer.java`
**GitHub:** [FakePlayer.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayer.java)

Mojang-mapped `ServerPlayer` subclass. The constructor uses `ClientInformation.createDefault()` and restores the target world with `setServerLevel(world)` (the packet-listener constructor overrides the level to overworld). The dummy connection uses `net.minecraft.network.Connection` (fully qualified — the simple name is shadowed by `WaypointTransmitter.Connection`). Movement uses `getDeltaMovement()`/`setDeltaMovement()` and `moveLocation` falls back to `setPos` + `setYRot`/`setXRot`/`setYHeadRot` because `Entity.moveTo` no longer exists.

| Member | Type | Purpose |
|---|---|---|
| `isFake` | `boolean` | Marker flag |
| `setupDummyConnection(server)` | `private void` | Fake connection install |
| `setMovementInput(f, s, j)` | `void` | Manual input |
| `applyManualMovement()` | `private void` | Velocity from input |
| `getItemInMainHand()` | `ItemStack` | Main-hand item |
| `lookAt(x, y, z)` | `void` | Face coordinate |
| `getServer()` | `MinecraftServer` | `MinecraftServer.getServer()` |
| `getHurtTicks()` | `int` | `hurtTime` |

```java
public FakePlayer(MinecraftServer server, ServerLevel world, GameProfile profile) {
    super(server, world, profile, ClientInformation.createDefault());
    setupDummyConnection(server);
    // The ServerGamePacketListenerImpl constructor overrides the player level to overworld
    // (player.setServerLevel(server.overworld())). Restore the target world so registration
    // and despawn stay consistent with the world where the bot was spawned.
    this.setServerLevel(world);
}
```

#### FakePlayerConnection.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayerConnection.java`
**GitHub:** [FakePlayerConnection.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayerConnection.java)

`ServerGamePacketListenerImpl` subclass that no-ops `send`, `disconnect`, `disconnectAsync`, and returns `null` for the Paper API connection accessors, so the bot never crashes and is never kicked from the player list.

| Method | Purpose |
|---|---|
| `send(Packet<?>)` | no-op |
| `disconnect(Component)` / `disconnectAsync(DisconnectionDetails)` | no-op |
| `getApiConnection()` | returns `null` |
| `paperConnection()` | returns `null` |

```java
public FakePlayerConnection(MinecraftServer server,
                            Connection connection,
                            ServerPlayer player) {
    super(server, connection, player,
            CommonListenerCookie.createInitial(player.getGameProfile(), false));
}

@Override
public void send(Packet<?> packet) {
    // no-op: the bot has no real client
}
```

#### FakePlayerFactory.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayerFactory.java`
**GitHub:** [FakePlayerFactory.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/nms/FakePlayerFactory.java)

Factory for the 1.21.11 fake players. Spawn registers into `PlayerList.players` and `ServerLevel.addFreshEntity`, broadcasting `ClientboundPlayerInfoUpdatePacket` (ADD_PLAYER) plus a manually built `ClientboundAddEntityPacket` (the `ClientboundAddPlayerPacket` constructor was merged away since 1.21.2). Despawn broadcasts `ClientboundRemoveEntitiesPacket` + `ClientboundPlayerInfoRemovePacket` and removes the entity.

| Method | Purpose |
|---|---|
| `spawn(name, world, x, y, z, yaw, pitch)` | Create + register + broadcast |
| `buildSpawnPacket(fake)` | Raw `ClientboundAddEntityPacket` |
| `despawn(FakePlayer)` | Broadcast removal + unregister |
| `despawnAll()` | Clean up all worlds |
| `broadcastPacket(Packet)` | Send to all online players |

```java
private static Packet<?> buildSpawnPacket(FakePlayer fake) {
    return new ClientboundAddEntityPacket(
            fake.getId(), fake.getUUID(),
            fake.getX(), fake.getY(), fake.getZ(),
            fake.getXRot(), fake.getYRot(),
            EntityType.PLAYER, 0,
            fake.getDeltaMovement(), fake.getYHeadRot());
}
```

### ViaVersion Integration

#### ViaCompat.java

**File:** `Plugins/1.21.11/src/main/java/com/carpetplayers/via/ViaCompat.java`
**GitHub:** [ViaCompat.java](https://github.com/HidayatBelajar319/CarpetPlayersMod-Plugins/blob/main/Plugins/1.21.11/src/main/java/com/carpetplayers/via/ViaCompat.java)

Helper for ViaVersion/ViaBackwards integration. Detects the actual client protocol version (e.g. a 1.16.5 client joining a 1.21.11 server through ViaBackwards) so the plugin/AI can adapt. Used by `/carpetplayers protocol`.

| Method | Purpose |
|---|---|
| `isAvailable()` | ViaVersion plugin present |
| `getProtocolVersion(Player)` / `getProtocolVersion(UUID)` | Client protocol int (0 if unknown) |
| `getClientVersionName(Player)` | Readable version name |
| `isLegacyClient(Player)` | Protocol < 767 (1.21.0) |

```java
public static boolean isLegacyClient(Player player) {
    int protocol = getProtocolVersion(player);
    if (protocol == 0) {
        return false;
    }
    // 767 = 1.21.0; anything below is considered a legacy version vs the 1.21.11 server
    return protocol < 767;
}
```