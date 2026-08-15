package com.carpetplayers.ai;

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
