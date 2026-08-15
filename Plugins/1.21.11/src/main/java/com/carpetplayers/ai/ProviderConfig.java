package com.carpetplayers.ai;

import java.util.ArrayList;
import java.util.List;

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
