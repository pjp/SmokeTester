package com.pearceful.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Created by ppearce on 2016-02-04.
 */
public class ShellScriptProcessor extends BaseSmokeTestStrategy {
    private SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_FAIL;
    private String cmdLine              = "";
    private long elapsedNs              = 0 ;
    private String msg                  = "";

    public ShellScriptProcessor(final int lineNumber, final String cmdLine) {
        this.cmdLine    = cmdLine;
        id              = ""+ lineNumber;
    }

    @Override
    public void execute() throws SmokeTestException {
        Runtime rt = Runtime.getRuntime();

        long startNs = System.nanoTime();

        ///////////////////////////
        // Process the command line
        try {
            Process proc = rt.exec(cmdLine);

            proc.waitFor();

            elapsedNs = System.nanoTime() - startNs;

            msg = gatherOutputs(proc, elapsedNs);

            if(proc.exitValue() == 0) {
                state = SmokeTestResult.STATE.USER_PASS;
            }
        } catch (IOException e) {
            elapsedNs = System.nanoTime() - startNs;

            msg = cmdDetails(0, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        } catch (InterruptedException e) {
            elapsedNs = System.nanoTime() - startNs;

            msg = cmdDetails(0, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        }
    }

    @Override
    public SmokeTestResult validate() {
        SmokeTestResult result  = new SmokeTestResult(id, state, elapsedNs, msg);

        return result;
    }

    protected String gatherOutputs(final Process proc, final long elapsedNs) {
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        return String.format(
                "%s, stdout [%s], stderr [%s]",
                cmdDetails(proc.exitValue(), id, cmdLine, elapsedNs),
                stdout.lines().collect(Collectors.joining("\n")),
                stderr.lines().collect(Collectors.joining("\n")));
    }

    protected String cmdDetails(final int exitCode, final String id, final String cmdLine, final long elapsedNs) {
        return String.format(
                "%s:, line [%s], cmd [%s], elapsedNs [%d]",
                (exitCode == 0 ? "PASS" : "FAIL"),
                id,
                cmdLine,
                elapsedNs);
    }
}
