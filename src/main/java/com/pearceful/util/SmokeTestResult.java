package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * An immutable result
 */
public class SmokeTestResult implements Comparable<SmokeTestResult> {
    private String id;          // Unique id of the test context this result is from.
    private STATE state;        // The state of the test result
    private long elapsedNanoSeconds;
    private String message;     // A result (or exception message)

    public enum STATE {
        ERROR,
        PASS,
        FAIL
    }

    public SmokeTestResult(
            final String id,
            final STATE state,
            final long elapsedNanoSeconds,
            final String message) {

        ////////////////
        // Sanity checks
        if(null == id || id.length() < 1) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }

        this.id                 = id;
        this.state              = state;
        this.elapsedNanoSeconds = elapsedNanoSeconds;
        this.message            = message;
    }

    public String getId() {
        return id;
    }

    public STATE getState() {
        return state;
    }

    public long getElapsedNanoSeconds() { return elapsedNanoSeconds; }

    public String getMessage() {
        return message;
    }

    public int compareTo(SmokeTestResult o) {
        return getId().compareTo(o.getId());
    }
}
