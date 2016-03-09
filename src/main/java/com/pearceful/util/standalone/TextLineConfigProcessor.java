package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;
import com.pearceful.util.SmokeTestException;
import com.pearceful.util.SmokeTestResult;
import com.pearceful.util.SmokeTestStrategy;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Created by Paul Pearce on 2016-01-04.
 *
 * Read a file that contains commands or shell scripts (one per line) to execute
 * in parallel.
 *
 * Example file is scripts-xxx.txt which contains for information about how
 * to configure and what to run.
 *
 */
public class TextLineConfigProcessor extends ConfigProcessor {
    public static final String VERSION              = "1.1";

    public static final String ENV_VARIABLE_NAME_PREFIX_KEY = "st.env.name.prefix";
    public static final String ENV_VARIABLE_NAME_PREFIX     = "ST_";
    public static final String ENV_VARIABLE_TAG_SUFFIX      = "TAG";
    public static final String ENV_VARIABLE_VALUE_SUFFIX    = "VALUE";
    public static final String ENV_VARIABLE_OS_SUFFIX       = "OS";
    public static final String ENV_VARIABLE_LINE_SUFFIX     = "LINE";

    public static final String TIMEOUT_SECONDS      = "TIMEOUT_SECONDS";
    public static final String THREAD_POOL_SIZE     = "THREAD_POOL_SIZE";
    public static final int    INT_VALUE_NOT_SET    = -1;

    private static final Logger LOGGER                  = Logger.getLogger(TextLineConfigProcessor.class);
    private static String envVariableNamePrefix         = ENV_VARIABLE_NAME_PREFIX;

    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 5;
        int timeoutSeconds  = 600;
        String filter       = null;
        TestSelectionFilter lineFilter   =   null;

        if(args.length < 2) {
            exitStatus      = 1;

            showUsage(exitStatus, "Need a file name to read as input and an selector tag!");
        }

        Path path       = Paths.get(args[0]);
        String stTag    = args[1];

        if(args.length > 2) {
            filter  = args[2];

            lineFilter = new TestSelectionFilter(filter);
        }

        if(null == stTag || stTag.trim().length() < 1) {
            exitStatus      = 2;

            showUsage(exitStatus, "No selector tag defined!") ;

        }

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        String envValueToBeSet  =    null;

        /////////////////////////////////////////
        // Process each non comment or blank line
        System.out.println("VERS: " + VERSION);
        System.out.println("COMM: ################################################");

        LOGGER.debug(
                String.format("main: input [%s], tag [%s], filter [%s]",
                        path.getFileName(),
                        stTag,
                        (null != lineFilter ? filter : "{NONE-SPECIFIED}")));

        int failedCount = 0;
        int passedCount = 0;
        int globalValue = -1;

        long start      = System.nanoTime();

        // Determine if the env. variable name prefix has been overridden
        LOGGER.debug(
                String.format(
                        "main: Checking for overriding SmokeTester env. name prefix [%s] system property",
                        ENV_VARIABLE_NAME_PREFIX_KEY));

        String propVal = System.getProperty(ENV_VARIABLE_NAME_PREFIX_KEY);

        if(null != propVal) {
            envVariableNamePrefix = propVal;
            LOGGER.debug("main: Overriding SmokeTester env. variable name prefix with [" + propVal + "]");
        } else {
            LOGGER.debug("main: Keeping SmokeTester env. variable name prefix [" + ENV_VARIABLE_NAME_PREFIX + "]");
        }

        LOGGER.info(
                String.format("main: Enviromental variable names available: [%s], [%s], [%s], [%s]",
                        buildEnvVariableName(ENV_VARIABLE_TAG_SUFFIX),
                        buildEnvVariableName(ENV_VARIABLE_VALUE_SUFFIX),
                        buildEnvVariableName(ENV_VARIABLE_OS_SUFFIX),
                        buildEnvVariableName(ENV_VARIABLE_LINE_SUFFIX)));

        try{
            List<String> lines  = Files.readAllLines(path);
            int lineNumber      = 0;

            for(String line : lines) {
                lineNumber++;

                globalValue = getGlobalIntSetting(line, TIMEOUT_SECONDS);
                if(INT_VALUE_NOT_SET != globalValue) {
                    timeoutSeconds = globalValue;
                    LOGGER.debug("main: Setting timeout to " + globalValue + " from line " + lineNumber);

                    continue;
                }

                globalValue = getGlobalIntSetting(line, THREAD_POOL_SIZE);
                if(INT_VALUE_NOT_SET != globalValue) {
                    threadPoolSize = globalValue;
                    LOGGER.debug("main: Setting threadPoolSize to " + globalValue + " from line " + lineNumber);

                    continue;
                }

                ////////////////////////////////////////////////////////////////////
                // Determine if a matching variable for the environment has been set
                String possibleEnvValueToBeSet  = valueToBeSelected(line, stTag);
                if(null != possibleEnvValueToBeSet) {
                    envValueToBeSet = possibleEnvValueToBeSet;
                }

                ////////////////////////////////////////////
                // Start the selection process for this line
                boolean selected        = lineToBeSelected(lineNumber, line, stTag);
                boolean passedFilter    = false;

                if(selected) {
                    //////////////////////////
                    // Matched the environment
                    String cmdLine = stripLeadingToken(line);

                    if (null != lineFilter) {
                        //////////////////////////////////////
                        // A command line filter was specified
                        passedFilter = lineFilter.isMatch(lineNumber, cmdLine);
                    } else {
                        passedFilter = true;
                    }

                    /////////////////////////////////////////////////////////////
                    // Finally check if the line passed all checks to be executed
                    if(passedFilter) {
                        shellScripts.add(
                                new TextLineTestProcessor(lineNumber, cmdLine, stTag, envValueToBeSet));
                    }
                }

                LOGGER.debug(
                        String.format(
                                "main: selected [%-5s], passedFilter [%-5s], line [%4d], envValue [%s], cmd [%s]",
                                selected,
                                passedFilter,
                                lineNumber,
                                envValueToBeSet,
                                line));
            }

            //////////////////////////////
            // Run the scripts in parallel
            List<SmokeTestResult> results =
                    SmokeTestContext.runSmokeTests(
                            shellScripts, threadPoolSize, timeoutSeconds);

            failedCount = processResults(results);
            passedCount = results.size() - failedCount;

            ////////////////////////////////////////////////
            // Indicate if there was a failure to the caller
            if(failedCount > 0) {
                exitStatus = 5;
            }
        } catch (IOException e) {
            System.err.println(e);
            exitStatus = 4;
        } catch (SmokeTestException e) {
            System.err.println(e);
            exitStatus = 3;
        }

        processSummaryAndExit(LOGGER, exitStatus, failedCount, passedCount, start);
    }

    private static int getGlobalIntSetting(final String line, final String name) {
        int value = INT_VALUE_NOT_SET;

        String globalValue = getGlobalStringSetting(line, name);

        if(null != globalValue) {
            value = Integer.parseInt(globalValue.trim());
        }

        return value ;
    }

    private static String getGlobalStringSetting(final String line, final String name) {
        String value = null;

        if(null == line)        return null;

        if(line.length() < 5)   return null;

        if (line.startsWith(COMMENT_LEADER + GLOBAL_SENTINAL)) {
            ////////////////////////////////////////
            // Extract the first token from the line
            StringTokenizer st = new StringTokenizer(line.substring(2), GLOBAL_SENTINAL);

            if (st.countTokens() < 2) return null;

            ///////////////////////////////////
            // Extract the global variable name
            String globalName   = st.nextToken();
            if(name.toLowerCase().equals(globalName.toLowerCase())) {
                // Matched what we are looking for, get it's value
                value = line.substring(name.length() + 3);
            }
        }

        return value ;
    }

    private static void showUsage(final int exitStatus, final String errMsg) {
        StringBuilder usage = new StringBuilder();

        usage.append("Usage for TextLineConfigProcessor - v" + VERSION + "\n");
        usage.append("\n");
        usage.append("TextLineConfigProcessor config selector_tag {filter}\n");
        usage.append("   config       = a file containing commands to execute.\n");
        usage.append("   selector_tag = a tag (case insensitive) to select specific lines from the config file to execute.\n");
        usage.append("   filter       = optional pattern to (possibly) reduce the cmd lines selected for execution.\n");
        usage.append("\n");
        usage.append("Notes:\n");
        usage.append("   the filter is of the format (1st characters denotes the type of filter):-\n");
        usage.append("      " + TestSelectionFilter.REGEX_FILTER_PREFIX + "cmd line contains this regex\n");
        usage.append("      " + TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED + "cmd line does NOT contain this regex\n");
        usage.append("      " + TestSelectionFilter.PLAIN_FILTER_PREFIX + "cmd line contains this text\n");
        usage.append("      " + TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED + "cmd line does NOT contain this text\n");
        usage.append("      " + TestSelectionFilter.TEST_ID_SENTINAL + "match this(these) line number(s)" + TestSelectionFilter.TEST_ID_SENTINAL + "\n");
        usage.append("\n");
        usage.append("   the filters are only applied if the line has ALREADY been selected by matching the selector tag.\n");
        usage.append("   the line number filter MUST end with a sentinal " +  TestSelectionFilter.TEST_ID_SENTINAL + ".\n");
        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("   TextLineConfigProcessor scripts.txt dev\n");
        usage.append("   TextLineConfigProcessor scripts.txt UAT\n");
        usage.append("   TextLineConfigProcessor scripts.txt sit  " + TestSelectionFilter.PLAIN_FILTER_PREFIX + "jsp\n");
        usage.append("   TextLineConfigProcessor scripts.txt PROD " + TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED + "admin\n");
        usage.append("   TextLineConfigProcessor scripts.txt sit  " + TestSelectionFilter.REGEX_FILTER_PREFIX + "a.+[\\d]{3}\n");
        usage.append("   TextLineConfigProcessor scripts.txt QA " + TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED + "A.+Servlet.*\n");

        usage.append("   TextLineConfigProcessor scripts.txt qa   "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "37"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        usage.append("   TextLineConfigProcessor scripts.txt uat  "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "39"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "47"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        String usageMsg    =   String.format("%s\n\n%s", errMsg, usage.toString());

        System.err.println(usageMsg);

        LOGGER.warn("showUsage: " + errMsg);

        doExit(LOGGER, exitStatus);
    }


    /**
     *
     * @param suffix
     * @return
     */
    protected static String buildEnvVariableName(final String suffix) {
        return envVariableNamePrefix + suffix;
    }
}
