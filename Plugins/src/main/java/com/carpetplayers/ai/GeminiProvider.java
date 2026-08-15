package com.carpetplayers.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiProvider extends AbstractAIProvider {

    private static final Gson GSON = new Gson();

    public GeminiProvider(ProviderConfig config) {
        super(config);
    }

    private String buildEndpoint(String model) {
        String base = getBaseUrl();
        if (base == null || base.isEmpty()) {
            base = "https://generativelanguage.googleapis.com";
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1beta/models/" + model + ":generateContent?key=" + getApiKey();
    }

    @Override
    public AIResponse sendMessage(List<AIMessage> messages, String model) throws AIException {
        String endpoint = buildEndpoint(model);
        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        for (AIMessage msg : messages) {
            if ("system".equalsIgnoreCase(msg.role)) {
                continue; // Gemini tidak punya role system di contents
            }
            JsonObject content = new JsonObject();
            String role = "user";
            if ("assistant".equalsIgnoreCase(msg.role) || "model".equalsIgnoreCase(msg.role)) {
                role = "model";
            }
            content.addProperty("role", role);
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", msg.content != null ? msg.content : "");
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
        }
        payload.add("contents", contents);

        AbstractAIProvider.HttpResult result = postJson(endpoint, GSON.toJson(payload), new HashMap<>());
        if (result.code < 200 || result.code >= 300) {
            throw classifyGeminiError(result.code, result.body);
        }
        try {
            JsonObject root = new JsonParser().parse(result.body).getAsJsonObject();
            JsonArray candidates = root.has("candidates") ? root.getAsJsonArray("candidates") : null;
            if (candidates == null || candidates.size() == 0) {
                throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                        "No candidates in Gemini response");
            }
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            StringBuilder text = new StringBuilder();
            for (JsonElement partEl : parts) {
                JsonObject partObj = partEl.getAsJsonObject();
                if (partObj.has("text")) {
                    text.append(partObj.get("text").getAsString());
                }
            }
            return new AIResponse(text.toString(), getName(), model, result.body);
        } catch (AIException e) {
            throw e;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                    "Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    @Override
    public AIResponse sendMessageWithTools(List<AIMessage> messages, List<AITool> tools,
                                           String model) throws AIException {
        if (tools == null || tools.isEmpty()) {
            return sendMessage(messages, model);
        }
        String endpoint = buildEndpoint(model);
        String json = buildGenerateContentPayload(messages, tools);

        AbstractAIProvider.HttpResult result = postJson(endpoint, json, new HashMap<>());
        if (result.code < 200 || result.code >= 300) {
            throw classifyGeminiError(result.code, result.body);
        }
        try {
            JsonObject root = new JsonParser().parse(result.body).getAsJsonObject();
            JsonArray candidates = root.has("candidates") ? root.getAsJsonArray("candidates") : null;
            if (candidates == null || candidates.size() == 0) {
                throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                        "No candidates in Gemini response");
            }
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            StringBuilder text = new StringBuilder();
            List<AIToolCall> toolCalls = new ArrayList<>();
            int index = 0;
            for (JsonElement partEl : parts) {
                JsonObject partObj = partEl.getAsJsonObject();
                if (partObj.has("text")) {
                    text.append(partObj.get("text").getAsString());
                }
                if (partObj.has("functionCall")) {
                    JsonObject call = partObj.getAsJsonObject("functionCall");
                    String name = call.has("name") ? call.get("name").getAsString() : "";
                    JsonObject args = call.has("args") && !call.get("args").isJsonNull()
                            ? call.getAsJsonObject("args") : new JsonObject();
                    toolCalls.add(new AIToolCall(name + "-" + index, name, args.toString()));
                    index++;
                }
            }
            return new AIResponse(text.toString(), getName(), model, result.body, toolCalls);
        } catch (AIException e) {
            throw e;
        } catch (Exception e) {
            throw new AIException(AIException.ErrorType.UNKNOWN, getName(), model, result.code,
                    "Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    /**
     * Membangun payload generateContent dengan dukungan function calling Gemini.
     * Pesan system pertama dipindah ke systemInstruction; pesan tool diterjemahkan
     * menjadi functionResponse dengan resolusi nama dari toolCalls sebelumnya.
     */
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
            JsonObject content = new JsonObject();
            if ("tool".equals(msg.role)) {
                content.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                JsonObject functionResponse = new JsonObject();
                String name = msg.toolCallId != null ? toolNames.get(msg.toolCallId) : null;
                functionResponse.addProperty("name", name != null ? name : "unknown_function");
                JsonObject response = new JsonObject();
                response.addProperty("result", msg.content != null ? msg.content : "");
                functionResponse.add("response", response);
                part.add("functionResponse", functionResponse);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                continue;
            }
            String role = "user";
            if ("assistant".equals(msg.role) || "model".equals(msg.role)) {
                role = "model";
            }
            content.addProperty("role", role);
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", msg.content != null ? msg.content : "");
            parts.add(textPart);
            if (("assistant".equals(msg.role) || "model".equals(msg.role)) && msg.toolCalls != null) {
                for (AIToolCall tc : msg.toolCalls) {
                    JsonObject callPart = new JsonObject();
                    JsonObject functionCall = new JsonObject();
                    functionCall.addProperty("name", tc.name);
                    functionCall.add("args", AITool.from(tc.arguments));
                    callPart.add("functionCall", functionCall);
                    parts.add(callPart);
                    if (tc.id != null) {
                        toolNames.put(tc.id, tc.name);
                    }
                }
            }
            content.add("parts", parts);
            contents.add(content);
        }
        payload.add("contents", contents);

        if (tools != null && !tools.isEmpty()) {
            JsonArray toolsArray = new JsonArray();
            JsonObject toolsEntry = new JsonObject();
            JsonArray declarations = new JsonArray();
            for (AITool tool : tools) {
                JsonObject declaration = new JsonObject();
                declaration.addProperty("name", tool.name);
                declaration.addProperty("description", tool.description != null ? tool.description : "");
                declaration.add("parameters", tool.parameters != null ? tool.parameters : new JsonObject());
                declarations.add(declaration);
            }
            toolsEntry.add("functionDeclarations", declarations);
            toolsArray.add(toolsEntry);
            payload.add("tools", toolsArray);
        }

        return GSON.toJson(payload);
    }

    private AIException classifyGeminiError(int code, String body) {
        try {
            JsonObject root = new JsonParser().parse(body).getAsJsonObject();
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                String status = error.has("status") ? error.get("status").getAsString() : "";
                String message = error.has("message") ? error.get("message").getAsString() : body;
                if ("RESOURCE_EXHAUSTED".equals(status)) {
                    return new AIException(AIException.ErrorType.QUOTA, getName(), null, code, message);
                }
                if ("PERMISSION_DENIED".equals(status) || "UNAUTHENTICATED".equals(status)) {
                    return new AIException(AIException.ErrorType.AUTH, getName(), null, code, message);
                }
                if ("NOT_FOUND".equals(status)) {
                    return new AIException(AIException.ErrorType.MODEL_NOT_FOUND, getName(), null, code, message);
                }
                if (code == 429) {
                    return new AIException(AIException.ErrorType.RATE_LIMIT, getName(), null, code, message);
                }
                return new AIException(AIException.ErrorType.HTTP, getName(), null, code, message);
            }
        } catch (Exception ignored) {
        }
        return classifyHttpError(code, body);
    }

    @Override
    public boolean testConnection() {
        try {
            List<AIMessage> messages = new ArrayList<>();
            messages.add(new AIMessage("user", "Katakan: OK"));
            AIResponse response = sendMessage(messages, getModels().isEmpty() ? "" : getModels().get(0));
            return response != null && response.content != null;
        } catch (AIException e) {
            lastError = e.getMessage();
            return false;
        }
    }
}
