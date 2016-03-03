package com.pearceful.util.standalone;

import com.pearceful.util.BaseSmokeTestStrategy;
import com.pearceful.util.SmokeTestException;
import com.pearceful.util.SmokeTestResult;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ppearce on 2016-03-02.
 */
public class JsonTestProcessor extends BaseSmokeTestStrategy {
    private SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_FAIL;

    private StandaloneJsonConfig.JsonSetup setup;
    private StandaloneJsonConfig.JsonTestDefinition testDef;
    private long elapsedNs              = 0 ;
    private String msg                  = "";
    private static final Logger LOGGER                  = Logger.getLogger(JsonTestProcessor.class);

    public static final String UNIX_SHELL           = "bash";
    public static final String UNIX_SHELL_PARAM     = "-c";

    public static final String WINDOWS_SHELL        = "cmd";
    public static final String WINDOWS_SHELL_PARAM  = "/c";

    public JsonTestProcessor(final StandaloneJsonConfig.JsonSetup setup,
                             final StandaloneJsonConfig.JsonTestDefinition testDef) {

        id              =   testDef.getId();
        this.setup      =   setup;
        this.testDef    =   testDef;
    }

    @Override
    public void execute() throws SmokeTestException {
        long startNs                = System.nanoTime();
        int exitValue               = -1;
        boolean runningInWindows    =  JsonConfigProcessor.onWindows();

        ///////////////////////////
        // Process the command line
        try {
            ProcessBuilder pb = null;

            if (runningInWindows) {
                pb = new ProcessBuilder(WINDOWS_SHELL, WINDOWS_SHELL_PARAM, testDef.getCmd());
                runningInWindows = true;
            } else {
                pb = new ProcessBuilder(UNIX_SHELL, UNIX_SHELL_PARAM, testDef.getCmd());
            }

            //////////////////////////////
            // Get the current environment
            Map<String, String> env = pb.environment();

            //////////////////////////////////////////
            // Set any bespoke environmental variables
            env.putAll(setup.getEnvronmentalVariables());

            ////////////////////////////////////////////////////////////////
            // Actually execute the command line and wait for it to complete
            Process proc    = pb.start();
            exitValue       = proc.waitFor();

            elapsedNs = System.nanoTime() - startNs;

            msg = gatherOutputs(proc, elapsedNs, testDef.getCmd());

            if(exitValue == 0) {
                // Strategy test PASSED
                state = SmokeTestResult.STATE.USER_PASS;
            }
        } catch (IOException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: id [%s %s]", id, testDef.getCmd()), e);

            msg = cmdDetails(exitValue, id, testDef.getCmd(), elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        } catch (InterruptedException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: id [%s %s]", id, testDef.getCmd()), e);

            msg = cmdDetails(exitValue, id, testDef.getCmd(), elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        }
    }

    @Override
    public SmokeTestResult validate() {
        // Simply create a Strategy result to indicate PASS or FAIL, the actual
        // validation was done in the execute method
        SmokeTestResult result  = new SmokeTestResult(id, state, elapsedNs, msg);

        LOGGER.trace(String.format("validate: result [%s] line [%s %s]", result, id, testDef.getCmd()));

        return result;
    }

    /**
     * Extract the outputs of the process.
     *
     * @param proc The process
     * @param elapsedNs how long the process executed for
     *
     * @return A String representation of the proc's output
     */
    protected String gatherOutputs(final Process proc, final long elapsedNs, final String cmdLine) {
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        return String.format(
                "%s, stdout [%s], stderr [%s]",
                cmdDetails(proc.exitValue(), id, cmdLine, elapsedNs),
                stdout.lines().collect(Collectors.joining("\n")),
                stderr.lines().collect(Collectors.joining("\n")));
    }

    /**
     * Extract the current process details.
     *
     * @param exitCode The proc's exit code
     * @param id The id of this Strategy (line number)
     * @param cmdLine The command line that was executed.
     * @param elapsedNs how long the process executed for
     *
     * @return A String representation of the process details
     */
    protected String cmdDetails(final int exitCode, final String id, final String cmdLine, final long elapsedNs) {
        return String.format(
                "%s: id [%s], cmd [%s], elapsedNs [%d]",
                (exitCode == 0 ? "PASS" : "FAIL"),
                id,
                cmdLine,
                elapsedNs);
    }

}
