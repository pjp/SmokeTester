package com.pearceful.util.standalone;

import com.pearceful.util.BaseSmokeTestStrategy;
import com.pearceful.util.SmokeTestException;
import com.pearceful.util.SmokeTestResult;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
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

        this.setup      =   setup;
        this.testDef    =   testDef;
    }

    @Override
    public void execute() throws SmokeTestException {
        long startNs    = System.nanoTime();
        int exitValue   = -1;
        boolean runningInWindows    =   false;

        ///////////////////////////
        // Process the command line
        try {
            ProcessBuilder pb = null;

            ////////////////////////////////////////////////////////
            // Quick and dirty test for determining the shell to use
            String osName = System.getProperty("os.name");
            osName = osName.toLowerCase(Locale.ENGLISH);

            if (osName.indexOf("windows") != -1) {
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
            env.putAll(setup.getSystemVariables());

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
        return null;
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
