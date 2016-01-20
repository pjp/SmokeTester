package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * A Smoke test will implement all these methods.
 */
public interface SmokeTestStrategy {
    public String           getId();        // The unique id of the test
    public void             preExecute();   // Called before the execute method to (possibly) set up some state
    public void             execute() throws SmokeTestException;    // Called to actually perform the smoke test.
    public void             postExecute();  // Called after the execute method to finalize any results.
    public SmokeTestResult  validate();     // Called to retrieve the result of running this test
    public boolean          wasCalled();    // Determine if the strategy was actually called for execution
    public void             setCalled();    // Set the called flag
}
