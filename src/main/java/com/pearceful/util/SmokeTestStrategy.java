package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 */
public interface SmokeTestStrategy {
    public String           getId();
    public void             preExecute();
    public void             execute() throws SmokeTestException;
    public void             postExecute();
    public SmokeTestResult  validate();
}
