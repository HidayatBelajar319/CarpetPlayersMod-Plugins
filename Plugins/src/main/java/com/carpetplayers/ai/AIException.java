package com.carpetplayers.ai;

public class AIException extends Exception {

    public enum ErrorType {
        AUTH,
        RATE_LIMIT,
        QUOTA,
        NETWORK,
        HTTP,
        MODEL_NOT_FOUND,
        NO_PROVIDER,
        UNKNOWN
    }

    public final ErrorType type;
    public final String providerName;
    public final String model;
    public final int statusCode;

    public AIException(ErrorType type, String providerName, String model, int statusCode, String message) {
        super(message);
        this.type = type;
        this.providerName = providerName;
        this.model = model;
        this.statusCode = statusCode;
    }

    public AIException(ErrorType type, String providerName, String model, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.providerName = providerName;
        this.model = model;
        this.statusCode = statusCode;
    }
}
