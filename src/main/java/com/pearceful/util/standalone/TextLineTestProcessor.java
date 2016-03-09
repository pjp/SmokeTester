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
 * Created by ppearce on 2016-01-04.
 *
 * An implementation of the SmokeTestStrategy that can be run
 * in parallel with other strategies.
 *
 * It's goal is to execute a command line and capture it's output
 * and exit status. If the exit status is !=0, then the strategy
 * will denote a failure of the test.
 *
 */
public class TextLineTestProcessor extends BaseSmokeTestStrategy {
    private SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_FAIL;
    private String cmdLine              = "";
    private String tag;
    private String envValue;
    private long elapsedNs              = 0 ;
    private String msg                  = "";
    private static final Logger LOGGER                  = Logger.getLogger(TextLineTestProcessor.class);

    public TextLineTestProcessor(
            final int lineNumber,
            final String cmdLine,
            final String tag,
            final String envValue) {
        this.cmdLine    = cmdLine.trim();
        id              = ""+ lineNumber;
        this.tag        = tag;
        this.envValue   = envValue;
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
            String osName   = System.getProperty("os.name");
            osName          = osName.toLowerCase(Locale.ENGLISH);

            if (osName.indexOf("windows") != -1) {
                pb = new ProcessBuilder(ConfigProcessor.WINDOWS_SHELL, ConfigProcessor.WINDOWS_SHELL_PARAM, cmdLine);
                runningInWindows    = true;
            } else {
                pb = new ProcessBuilder(ConfigProcessor.UNIX_SHELL, ConfigProcessor.UNIX_SHELL_PARAM, cmdLine);
            }

            //////////////////////////////
            // Get the current environment
            Map<String, String> env = pb.environment();

            //////////////////////////////////////////
            // Set any bespoke environmental variables
            setEnvVariables(env, runningInWindows, osName);

            ////////////////////////////////////////////////////////////////
            // Actually execute the command line and wait for it to complete
            Process proc    = pb.start();
            exitValue       = proc.waitFor();

            elapsedNs = System.nanoTime() - startNs;

            msg = gatherOutputs(proc, elapsedNs);

            if(exitValue == 0) {
                // Strategy test PASSED
                state = SmokeTestResult.STATE.USER_PASS;
            }
        } catch (IOException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: line [%s %s]", id, cmdLine), e);

            msg = cmdDetails(exitValue, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        } catch (InterruptedException e) {
            elapsedNs = System.nanoTime() - startNs;

            LOGGER.error(String.format("execute: line [%s %s]", id, cmdLine), e);

            msg = cmdDetails(exitValue, id, cmdLine, elapsedNs) + ", ERROR: " + e.toString();

            state = SmokeTestResult.STATE.EXEC_ERROR;
        }
    }

   @Override
    public SmokeTestResult validate() {
       // Simply create a Strategy result to indicate PASS or FAIL, the actual
       // validation was done in the execute method
       SmokeTestResult result  = new SmokeTestResult(id, state, elapsedNs, msg);

       LOGGER.trace(String.format("validate: result [%s] line [%s %s]", result, id, cmdLine));

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
    protected String gatherOutputs(final Process proc, final long elapsedNs) {
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        return String.format(
                "%s, stdout [%s], stderr [%s]",
                cmdDetails(proc.exitValue(), id, cmdLine, elapsedNs),
                stdout.lines().collect(Collectors.joining("\n")),
                stderr.lines().collect(Collectors.joining("\n")));
    }

    /**
     * Set smokeTester environmental varuiablesd if they don't already exist
     *
     * @param env The map of current environmental variables (will be added to)
     * @param runningInWindows flag to say what OS we are running in
     * @param osName The name of the current OS
     */
    protected void setEnvVariables(Map<String, String> env, boolean runningInWindows, String osName) {
        ///////////////////////////////////////////////
        // Add to the environment our bespoke variables
        String key = TextLineConfigProcessor.buildEnvVariableName(TextLineConfigProcessor.ENV_VARIABLE_TAG_SUFFIX);

        String existingValue = env.putIfAbsent(key, tag);

        if(null != existingValue) {
            LOGGER.warn(String.format("setEnvVariables: Env. variable [%s] already exists with value [%s]", key, existingValue));
        }

        /////////////////////////////
        if (null != envValue) {
            key = TextLineConfigProcessor.buildEnvVariableName(TextLineConfigProcessor.ENV_VARIABLE_VALUE_SUFFIX);

            existingValue = env.putIfAbsent(key, envValue);

            if(null != existingValue) {
                LOGGER.warn(String.format("setEnvVariables: Env. variable [%s] already exists with value [%s]", key, existingValue));
            }
        }

        /////////////////////////////
        key = TextLineConfigProcessor.buildEnvVariableName(TextLineConfigProcessor.ENV_VARIABLE_OS_SUFFIX);

        if(runningInWindows) {
            existingValue = env.putIfAbsent(key, "windows");
        } else {
            existingValue = env.putIfAbsent(key, "unix");
        }

        if(null != existingValue) {
            LOGGER.warn(String.format("setEnvVariables: Env. variable [%s] already exists with value [%s]", key, existingValue));
        }

        /////////////////////////////
        key = TextLineConfigProcessor.buildEnvVariableName(TextLineConfigProcessor.ENV_VARIABLE_LINE_SUFFIX);

        existingValue = env.putIfAbsent(key, id);

        if(null != existingValue) {
            LOGGER.warn(String.format("setEnvVariables: Env. variable [%s] already exists with value [%s]", key, existingValue));
        }
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
                "%s: line [%s], cmd [%s], elapsedNs [%d]",
                (exitCode == 0 ? "PASS" : "FAIL"),
                id,
                cmdLine,
                elapsedNs);
    }
}
