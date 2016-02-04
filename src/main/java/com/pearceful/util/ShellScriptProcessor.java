package com.pearceful.util;

import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by ppearce on 2016-02-04.
 */
public class ShellScriptProcessor extends BaseSmokeTestStrategy {
    private SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_FAIL;
    private String cmdLine              = "";
    private long elapsedNs              = 0 ;
    private String msg                  = "";

    public ShellScriptProcessor(final String cmdLine) {
        this.cmdLine = cmdLine;

        id  =   "" + cmdLine.hashCode();
    }

    @Override
    public void execute() throws SmokeTestException {
        Runtime rt = Runtime.getRuntime();

        long startNs = System.nanoTime();

        ///////////////////////////
        // Process the command line
        try {
            Process proc = rt.exec(cmdLine);
            elapsedNs = System.nanoTime() - startNs;

            msg = gatherOutputs(proc, elapsedNs);

            if(proc.exitValue() == 0) {
                state = SmokeTestResult.STATE.USER_PASS;
            }
        } catch (IOException e) {
            elapsedNs = System.nanoTime() - startNs;

            msg = cmdDetails(id, 99, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        }
    }

    @Override
    public SmokeTestResult validate() {
        SmokeTestResult result  = new SmokeTestResult(id, state, elapsedNs, msg);

        return result;
    }

    protected String gatherOutputs(final Process proc, final long elapsedNs) {
        return String.format(
                "%s, stdout [%s], stderr [%s]",
                cmdDetails(id, proc.exitValue(), cmdLine, elapsedNs),
                proc.getOutputStream().toString(),
                proc.getErrorStream().toString());
    }

    protected String cmdDetails(final String id, final int exitCode, final String cmdLine, final long elapsedNs) {
        return String.format(
                "id [%s], cmd [%s], elapsedNs [%d]",
                (exitCode == 0 ? "PASS" : "FAIL"),
                cmdLine,
                elapsedNs);
    }
}
