package com.carpetplayers.ai;

import com.carpetplayers.bot.BotBrain;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AITool {
    public final String name;
    public final String description;
    public final JsonObject parameters; // JSON Schema for args

    public interface Executor {
        String execute(JsonObject args, BotBrain bot) throws Exception;
    }

    private final Executor executor;

    public AITool(String name, String description, JsonObject parameters, Executor executor) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.executor = executor;
    }

    public String execute(JsonObject args, BotBrain bot) throws Exception {
        return executor.execute(args, bot);
    }

    /**
     * Parses a raw JSON argument string into a JsonObject.
     * Empty, null or invalid input yields an empty object.
     */
    public static JsonObject from(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new JsonObject();
        }
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    /**
     * Builds an object-style JSON Schema from the given parameter definitions
     * (as produced by stringParam/intParam/doubleParam/booleanParam/enumParam).
     */
    public static JsonObject objectParams(JsonObject... params) {
        JsonObject result = new JsonObject();
        result.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        if (params != null) {
            for (JsonObject param : params) {
                if (param == null || !param.has("name") || !param.has("schema")) {
                    continue;
                }
                String name = param.get("name").getAsString();
                properties.add(name, param.getAsJsonObject("schema"));
                if (param.has("required") && param.get("required").getAsBoolean()) {
                    required.add(name);
                }
            }
        }
        result.add("properties", properties);
        if (required.size() > 0) {
            result.add("required", required);
        }
        return result;
    }

    /** Empty parameters schema for tools without arguments. */
    public static JsonObject noParams() {
        return objectParams();
    }

    public static JsonObject stringParam(String name, String description, boolean required) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        if (description != null) {
            schema.addProperty("description", description);
        }
        return param(name, schema, required);
    }

    public static JsonObject intParam(String name, String description, boolean required,
                                      int defaultValue, int min, int max) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "integer");
        if (description != null) {
            schema.addProperty("description", description);
        }
        schema.addProperty("minimum", min);
        schema.addProperty("maximum", max);
        return param(name, schema, required);
    }

    public static JsonObject doubleParam(String name, String description, boolean required) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "number");
        if (description != null) {
            schema.addProperty("description", description);
        }
        return param(name, schema, required);
    }

    public static JsonObject booleanParam(String name, String description, boolean required) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "boolean");
        if (description != null) {
            schema.addProperty("description", description);
        }
        return param(name, schema, required);
    }

    public static JsonObject enumParam(String name, String description, boolean required, String... values) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        if (description != null) {
            schema.addProperty("description", description);
        }
        JsonArray enums = new JsonArray();
        if (values != null) {
            for (String value : values) {
                enums.add(value);
            }
        }
        schema.add("enum", enums);
        return param(name, schema, required);
    }

    private static JsonObject param(String name, JsonObject schema, boolean required) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("name", name);
        wrapper.add("schema", schema);
        wrapper.addProperty("required", required);
        return wrapper;
    }
}
