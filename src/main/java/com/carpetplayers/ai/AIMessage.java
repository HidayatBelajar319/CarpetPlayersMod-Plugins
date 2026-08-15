package com.carpetplayers.ai;

import java.util.List;

public class AIMessage {
    public final String role;
    public final String content;
    public final String toolCallId;
    public final List<AIToolCall> toolCalls;

    public AIMessage(String role, String content) {
        this(role, content, null, null);
    }

    public AIMessage(String role, String content, String toolCallId, List<AIToolCall> toolCalls) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolCalls = toolCalls;
    }

    public static AIMessage system(String content) {
        return new AIMessage("system", content);
    }

    public static AIMessage user(String content) {
        return new AIMessage("user", content);
    }

    public static AIMessage assistant(String content) {
        return new AIMessage("assistant", content);
    }

    public static AIMessage tool(String toolCallId, String content) {
        return new AIMessage("tool", content, toolCallId, null);
    }

    public static AIMessage assistantWithTools(String content, List<AIToolCall> toolCalls) {
        return new AIMessage("assistant", content, null, toolCalls);
    }
}
