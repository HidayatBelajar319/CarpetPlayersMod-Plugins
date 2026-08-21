package com.carpetplayers.ai;

import com.carpetplayers.CarpetPlayersMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAICompatibleProvider extends AbstractAIProvider {

    private static final Gson GSON = new Gson();

    public OpenAICompatibleProvider(ProviderConfig config) {
        super(config);
    }

    private String buildEndpoint() {
        String base = getBaseUrl();
        if (base == null || base.isEmpty()) {
            if ("openrouter".equalsIgnoreCase(getType())) {
                base = "https://openrouter.ai/api/v1";
            } else if ("groq".equalsIgnoreCase(getType())) {
                base = "https://api.groq.com/openai/v1";
            } else {
                base = "https://api.openai.com/v1";
            }
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }

    @Override
    public AIResponse sendMessage(List<AIMessage> messages, String model) throws AIException {
        return doSend(messages, null, model);
    }

    @Override
    public AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools,
                                           String model) throws AIException {
        if (tools == null || tools.isEmpty()) {
            return sendMessage(messages, model);
        }
        return doSend(messages, tools, model);
    }

    private AIResponse doSend(List<AIMessage> messages, List<AITool> tools, String model) throws AIException {
        String endpoint = buildEndpoint();
        String json = buildPayload(messages, tools, model);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + getApiKey());

        AbstractAIProvider.HttpResult result = postJson(endpoint, json, headers);
        if (result.code < 200 || result.code >= 300) {
            throw classifyHttpError(result.code, result.body);
        }
        try {
            JsonObject root = new JsonParser().parse(result.body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                        "No choices in response");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            String content = message.has("content") && !message.get("content").isJsonNull()
                    ? message.get("content").getAsString() : "";
            List<AIToolCall> toolCalls = parseToolCalls(message);
            AIResponse response = new AIResponse(content, getName(), model, result.body, toolCalls);
            if (root.has("usage")) {
                JsonObject usage = root.getAsJsonObject("usage");
                if (usage.has("prompt_tokens")) response.promptTokens = usage.get("prompt_tokens").getAsInt();
                if (usage.has("completion_tokens")) response.completionTokens = usage.get("completion_tokens").getAsInt();
                if (usage.has("total_tokens")) response.totalTokens = usage.get("total_tokens").getAsInt();
            }
            return response;
        } catch (AIException e) {
            throw e;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    private String buildPayload(List<AIMessage> messages, List<AITool> tools, String model) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);

        JsonArray messageArray = new JsonArray();
        for (AIMessage msg : messages) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.role);
            m.addProperty("content", msg.content != null ? msg.content : "");
            if ("tool".equals(msg.role) && msg.toolCallId != null) {
                m.addProperty("tool_call_id", msg.toolCallId);
            }
            if ("assistant".equals(msg.role) && msg.toolCalls != null && !msg.toolCalls.isEmpty()) {
                JsonArray calls = new JsonArray();
                for (AIToolCall call : msg.toolCalls) {
                    JsonObject callObj = new JsonObject();
                    callObj.addProperty("id", call.id);
                    callObj.addProperty("type", "function");
                    JsonObject function = new JsonObject();
                    function.addProperty("name", call.name);
                    function.addProperty("arguments", call.arguments != null ? call.arguments : "{}");
                    callObj.add("function", function);
                    calls.add(callObj);
                }
                m.add("tool_calls", calls);
            }
            messageArray.add(m);
        }
        payload.add("messages", messageArray);

        if (tools != null && !tools.isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            for (AITool tool : tools) {
                JsonObject toolObj = new JsonObject();
                toolObj.addProperty("type", "function");
                JsonObject function = new JsonObject();
                function.addProperty("name", tool.name);
                function.addProperty("description", tool.description != null ? tool.description : "");
                function.add("parameters", tool.parameters != null ? tool.parameters : new JsonObject());
                toolObj.add("function", function);
                toolsArray.add(toolObj);
            }
            payload.add("tools", toolsArray);
        }

        return GSON.toJson(payload);
    }

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

    /**
     * Queries /v1/models endpoint to discover available models.
     * Works for OpenAI, Groq, OpenRouter, and any OpenAI-compatible provider.
     */
    @Override
    public List<String> fetchModels() {
        try {
            String base = getBaseUrl();
            if (base == null || base.isEmpty()) {
                if ("openrouter".equalsIgnoreCase(getType())) {
                    base = "https://openrouter.ai/api/v1";
                } else if ("groq".equalsIgnoreCase(getType())) {
                    base = "https://api.groq.com/openai/v1";
                } else {
                    base = "https://api.openai.com/v1";
                }
            }
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            String modelsUrl = base + "/models";
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + getApiKey());
            HttpResult result = getJson(modelsUrl, headers);
            if (result.code < 200 || result.code >= 300) {
                CarpetPlayersMod.LOGGER.warn("Failed to fetch models from {}: HTTP {}", modelsUrl, result.code);
                return new ArrayList<>();
            }
            JsonObject root = new JsonParser().parse(result.body).getAsJsonObject();
            JsonArray data = root.has("data") ? root.getAsJsonArray("data") : null;
            if (data == null) {
                return new ArrayList<>();
            }
            List<String> models = new ArrayList<>();
            for (JsonElement el : data) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("id")) {
                    models.add(obj.get("id").getAsString());
                }
            }
            CarpetPlayersMod.LOGGER.info("Discovered {} model(s) from {}", models.size(), getName());
            return models;
        } catch (Exception e) {
            CarpetPlayersMod.LOGGER.warn("Failed to fetch models from {}: {}", getName(), e.getMessage());
            return new ArrayList<>();
        }
    }
}
