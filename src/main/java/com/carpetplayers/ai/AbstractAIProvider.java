package com.carpetplayers.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractAIProvider implements AIProvider {

    protected final ProviderConfig config;
    protected volatile int failureCount = 0;
    protected volatile long cooldownUntil = 0;
    protected volatile String lastError = null;

    public AbstractAIProvider(ProviderConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return config.name != null ? config.name : config.type;
    }

    @Override
    public String getType() {
        return config.type != null ? config.type : "openai";
    }

    @Override
    public boolean isEnabled() {
        return config.enabled;
    }

    @Override
    public int getPriority() {
        return config.priority;
    }

    @Override
    public List<String> getModels() {
        if (config.models != null && !config.models.isEmpty()) {
            return new ArrayList<>(config.models);
        }
        if (config.model != null && !config.model.isEmpty()) {
            return Collections.singletonList(config.model);
        }
        return new ArrayList<>();
    }

    public String getApiKey() {
        return config.apiKey != null ? config.apiKey : "";
    }

    public String getBaseUrl() {
        return config.baseUrl != null ? config.baseUrl : "";
    }

    public int getTimeoutMs() {
        return config.timeoutMs > 0 ? config.timeoutMs : 30000;
    }

    @Override
    public ProviderHealth getHealth() {
        return new ProviderHealth(getName(), isEnabled(), onCooldown(), getPriority(),
                failureCount, cooldownUntil, lastError);
    }

    @Override
    public void markSuccess() {
        failureCount = 0;
        cooldownUntil = 0;
        lastError = null;
    }

    @Override
    public void markFailure(AIException exception) {
        failureCount++;
        cooldownUntil = System.currentTimeMillis() + 30000L;
        lastError = exception.getMessage();
    }

    @Override
    public boolean onCooldown() {
        return System.currentTimeMillis() < cooldownUntil;
    }

    protected AIException classifyHttpError(int code, String body) {
        String reason = body != null && !body.isEmpty() ? body : "HTTP " + code;
        switch (code) {
            case 401:
            case 403:
                return new AIException(AIException.ErrorType.AUTH, getName(), null, code,
                        "Authentication failed (" + code + "): " + reason);
            case 404:
                return new AIException(AIException.ErrorType.MODEL_NOT_FOUND, getName(), null, code,
                        "Model or endpoint not found (" + code + "): " + reason);
            case 429:
                return new AIException(AIException.ErrorType.RATE_LIMIT, getName(), null, code,
                        "Rate limited (" + code + "): " + reason);
            case 402:
                return new AIException(AIException.ErrorType.QUOTA, getName(), null, code,
                        "Quota exceeded (" + code + "): " + reason);
            case 500:
            case 502:
            case 503:
            case 504:
                return new AIException(AIException.ErrorType.HTTP, getName(), null, code,
                        "Provider server error (" + code + "): " + reason);
            default:
                return new AIException(AIException.ErrorType.HTTP, getName(), null, code,
                        "Unexpected HTTP error (" + code + "): " + reason);
        }
    }

    protected HttpResult postJson(String url, String jsonBody, Map<String, String> headers) throws AIException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(getTimeoutMs());
            connection.setReadTimeout(getTimeoutMs());
            connection.setRequestProperty("Content-Type", "application/json");
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.setDoOutput(true);
            if (jsonBody != null) {
                byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            return new HttpResult(code, body);
        } catch (java.net.ConnectException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "Cannot connect to " + url + ": " + e.getMessage(), e);
        } catch (java.net.SocketTimeoutException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "Timeout connecting to " + url + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "I/O error talking to " + url + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    protected HttpResult getJson(String url, Map<String, String> headers) throws AIException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(getTimeoutMs());
            connection.setReadTimeout(getTimeoutMs());
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(stream);
            return new HttpResult(code, body);
        } catch (java.net.ConnectException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "Cannot connect to " + url + ": " + e.getMessage(), e);
        } catch (java.net.SocketTimeoutException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "Timeout connecting to " + url + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AIException(AIException.ErrorType.NETWORK, getName(), null, 0,
                    "I/O error talking to " + url + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Queries the provider's model listing endpoint and returns available model IDs.
     * Default implementation returns an empty list; subclasses override.
     */
    public List<String> fetchModels() {
        return new ArrayList<>();
    }

    protected static class HttpResult {
        public final int code;
        public final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
