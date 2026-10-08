package com.fitness.aiservice.service;

/** The model gave no usable recommendation. The reason is stored on the fallback recommendation. */
public class AiResponseException extends RuntimeException {

    public enum Reason {
        /** GEMINI_KEY or GEMINI_URL is not set. */
        NOT_CONFIGURED,
        TIMEOUT,
        /** The request did not complete (connection refused, DNS, TLS). */
        UNAVAILABLE,
        /** Gemini answered with an HTTP error status. */
        MODEL_ERROR,
        /** No candidate text, for example because the prompt or the answer was blocked. */
        NO_CANDIDATES,
        /** The text was not the JSON structure the prompt asks for. */
        MALFORMED_RESPONSE
    }

    private final Reason reason;

    public AiResponseException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
