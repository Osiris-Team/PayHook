package com.osiris.payhook.exceptions;

public class InvalidChangeException extends Exception {

    public InvalidChangeException() {
    }

    public InvalidChangeException(String message) {
        super(message);
    }

    public InvalidChangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidChangeException(Throwable cause) {
        super(cause);
    }

    public InvalidChangeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
