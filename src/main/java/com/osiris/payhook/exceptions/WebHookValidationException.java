package com.osiris.payhook.exceptions;

/**
 * Thrown when the webhook couldn't be validated.
 */
public class WebHookValidationException extends Exception{
    public WebHookValidationException() {
    }

    public WebHookValidationException(String message) {
        super(message);
    }

    public WebHookValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebHookValidationException(Throwable cause) {
        super(cause);
    }

    public WebHookValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
