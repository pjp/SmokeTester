package com.pearceful.util;

/**
 * Created by ppearce on 2016-02-04.
 */
public class ShellScriptProcessor extends BaseSmokeTestStrategy{
    private String cmdLine;

    public ShellScriptProcessor(final String cmdLine) {
        this.cmdLine = cmdLine;
    }

    @Override
    public void execute() throws SmokeTestException {

    }

    @Override
    public SmokeTestResult validate() {
        return null;
    }
}
