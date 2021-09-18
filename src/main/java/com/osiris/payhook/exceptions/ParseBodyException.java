package com.osiris.payhook.exceptions;

public class ParseBodyException extends Exception {

    public ParseBodyException() {
    }

    public ParseBodyException(String message) {
        super(message);
    }

    public ParseBodyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseBodyException(Throwable cause) {
        super(cause);
    }

    public ParseBodyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
