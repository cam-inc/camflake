package com.camobile.camflake;

/**
 * Runtime exception of {@link Camflake}.
 */
public final class CamflakeException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public CamflakeException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public CamflakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
