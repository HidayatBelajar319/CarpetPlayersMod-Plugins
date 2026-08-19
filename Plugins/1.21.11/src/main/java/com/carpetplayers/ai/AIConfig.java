package com.carpetplayers.ai;

import java.util.ArrayList;
import java.util.List;

public class AIConfig {
    public boolean enabled = true;
    public boolean aiChatEnabled = true;
    public boolean aiDefensiveEnabled = true;
    public String systemPrompt = "You are the brain of a Minecraft bot (FakePlayer) named Carpet Players. "
            + "You control the bot's movement and actions inside the Minecraft 1.16.5 game. "
            + "Speak briefly and use English. "
            + "You can attack enemies, eat, walk, jump, and use items. "
            + "Use the available tools to control the bot, do not just tell stories.";
    public int requestTimeoutMs = 30000;
    public int failureCooldownMs = 30000;
    public boolean debugLogging = false;
    public List<ProviderConfig> providers = new ArrayList<>();
}
