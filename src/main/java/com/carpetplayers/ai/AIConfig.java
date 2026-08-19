package com.carpetplayers.ai;

import java.util.ArrayList;
import java.util.List;

public class AIConfig {
    public boolean enabled = true;
    public boolean aiChatEnabled = true;
    public boolean aiDefensiveEnabled = true;
    public String systemPrompt = "You are the AI brain of a Minecraft bot in Minecraft 1.16.5 (Fabric with Carpet mod). "
            + "You control the bot's movement, actions, and interactions using tools. "
            + "Always speak briefly in English. "
            + "You have access to tools: move, jump, sneak, look_at, attack, eat, chat, stop, set_state, "
            + "mine_block, use_item, drop_item, equip_kit, get_state, and run_command. "
            + "You can fight enemies, eat food, walk, jump, sneak, mine blocks, use items, and run server commands. "
            + "Use tools to actually control the bot — do NOT just describe what you would do. "
            + "When given a task, break it into steps and execute them with tools. "
            + "The run_command tool lets you execute server commands as the bot (e.g. /give, /effect, /tp). "
            + "Always check get_state first if you are unsure of the bot's current situation.";
    public int requestTimeoutMs = 30000;
    public int failureCooldownMs = 30000;
    public boolean debugLogging = false;
    public List<ProviderConfig> providers = new ArrayList<>();
}
