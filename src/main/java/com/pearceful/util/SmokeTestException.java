package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * A marker Exception class to wrap an Exception
 */
public class SmokeTestException extends Exception{

    public SmokeTestException(String message) {
        super(message);
    }

    public SmokeTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmokeTestException(Throwable cause) {
        super(cause);
    }
}
