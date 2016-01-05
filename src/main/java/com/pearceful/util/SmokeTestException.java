package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 */
public class SmokeTestException extends Exception{

    public SmokeTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmokeTestException(Throwable cause) {
        super(cause);
    }
}
