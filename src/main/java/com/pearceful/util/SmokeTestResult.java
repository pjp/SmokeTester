package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * An immutable result after trying to run a smoke test
 */
public class SmokeTestResult implements Comparable<SmokeTestResult> {
    private String id;                  // Unique id of the test context this result is from.
    private STATE state;                // The state of the test result
    private long elapsedNanoSeconds;    // How long the test actually ran for.
    private String message;             // A result (or exception message)

    public enum STATE {
        ERROR,  // A problem occurred while trying to run the test
        PASS,   // The test ran and (all) the test conditions passed
        FAIL    // The test ran, but the test condition(s) failed
    }

    /**
     *
     * @param id The id of the SmokeTestStrategy that produced this result, if it doesn't match, then the
     *           SmokeTestContext.runSmokeTests method will log an error and the result will will be discarded.
     * @param state
     * @param elapsedNanoSeconds
     * @param message
     */
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

    @Override
    public String toString() {
        return String.format(
                "id [%s], state [%s], elapsedNs [%d], message [%s]",
                id, state, elapsedNanoSeconds, message
        );
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
