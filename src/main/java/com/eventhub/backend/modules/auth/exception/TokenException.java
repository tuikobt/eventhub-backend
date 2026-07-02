package com.eventhub.backend.modules.auth.exception;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}
