package com.pearceful.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ppearce on 2016-02-04.
 */
public class ShellScriptProcessor extends BaseSmokeTestStrategy {
    private SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_FAIL;
    private String cmdLine              = "";
    private String envName;
    private String envValue;
    private long elapsedNs              = 0 ;
    private String msg                  = "";
    private static final Logger LOGGER                  = Logger.getLogger(ShellScriptProcessor.class);

    public ShellScriptProcessor(
            final int lineNumber,
            final String cmdLine,
            final String envName,
            final String envValue) {
        this.cmdLine    = cmdLine.trim();
        id              = ""+ lineNumber;
        this.envName    = envName;
        this.envValue   = envValue;
    }

    @Override
    public void execute() throws SmokeTestException {
        Runtime rt = Runtime.getRuntime();

        long startNs = System.nanoTime();

        ///////////////////////////
        // Process the command line
        try {
            // Need to get all the existing env variables and add two more
            Map<String, String> currentEnv = System.getenv();

            List<String> newEnvList = new ArrayList<>();

            if (null != envName) {
                newEnvList.add(ShellScriptListProcessor.TAG_ENV_NAME + "=" + envName);
            }

            if (null != envValue) {
                newEnvList.add(ShellScriptListProcessor.TAG_ENV_VALUE + "=" + envValue);
            }

            /////////////////////////////
            // Add the existing variables
            for(Map.Entry e : currentEnv.entrySet()) {
                newEnvList.add(e.getKey() + "=" + e.getValue());
            }

            ///////////////////////////////
            // Convert the List to an array
            String[] newEnv = newEnvList.toArray(new String[newEnvList.size()]);

            Process proc = rt.exec(cmdLine, newEnv);

            proc.waitFor();

            elapsedNs = System.nanoTime() - startNs;

            msg = gatherOutputs(proc, elapsedNs);

            if(proc.exitValue() == 0) {
                state = SmokeTestResult.STATE.USER_PASS;
            }
        } catch (IOException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: line [%s %s]", id, cmdLine), e);

            msg = cmdDetails(0, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        } catch (InterruptedException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: line [%s %s]", id, cmdLine), e);

            msg = cmdDetails(0, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        }
    }

    @Override
    public SmokeTestResult validate() {
        SmokeTestResult result  = new SmokeTestResult(id, state, elapsedNs, msg);

        LOGGER.trace(String.format("validate: result [%s] line [%s %s]", result, id, cmdLine));

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
