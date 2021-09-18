package com.osiris.payhook.exceptions;

public class ParseHeaderException extends Exception {
    public ParseHeaderException() {
    }

    public ParseHeaderException(String message) {
        super(message);
    }

    public ParseHeaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseHeaderException(Throwable cause) {
        super(cause);
    }

    public ParseHeaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
