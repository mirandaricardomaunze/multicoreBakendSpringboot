package com.phcpro.desktop.client;

public class ApiClientException extends RuntimeException {

    private final int statusCode;

    public ApiClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
