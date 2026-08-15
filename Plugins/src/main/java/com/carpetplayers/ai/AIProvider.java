package com.carpetplayers.ai;

import java.util.List;

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
