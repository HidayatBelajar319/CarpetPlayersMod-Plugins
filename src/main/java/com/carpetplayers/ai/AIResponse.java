package com.carpetplayers.ai;

import java.util.List;

public class AIResponse {
    public final String content;
    public final String providerName;
    public final String model;
    public final String raw;
    public final List<AIToolCall> toolCalls;

    public int promptTokens;
    public int completionTokens;
    public int totalTokens;

    public AIResponse(String content, String providerName, String model, String raw) {
        this(content, providerName, model, raw, null);
    }

    public AIResponse(String content, String providerName, String model, String raw,
                      List<AIToolCall> toolCalls) {
        this.content = content;
        this.providerName = providerName;
        this.model = model;
        this.raw = raw;
        this.toolCalls = toolCalls;
    }
}
