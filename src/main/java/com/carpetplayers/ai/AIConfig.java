package com.carpetplayers.ai;

import java.util.ArrayList;
import java.util.List;

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
