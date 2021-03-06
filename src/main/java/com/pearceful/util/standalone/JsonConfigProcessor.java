package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;
import com.pearceful.util.SmokeTestException;
import com.pearceful.util.SmokeTestResult;
import com.pearceful.util.SmokeTestStrategy;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Created by Paul Pearce on 2016-01-04.
 *
 * Read a file that contains commands to execute in parallel.
 *
 * Example file is sample-conf.json which contains for information about how
 * to configure and what to run.
 *
 */
public class JsonConfigProcessor extends ConfigProcessor {
    public static final String VERSION              = "1.1";

    private static final Logger LOGGER                  = Logger.getLogger(JsonConfigProcessor.class);

    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 5;
        int timeoutSeconds  = 600;
        String filter       = null;
        TestSelectionFilter selectionFilter   =   null;

        if(args.length < 2) {
            exitStatus      = 1;

            showUsage(exitStatus, "Need a file name to read as input and an selector tag!");
        }

        String jsonConfigFile   = args[0];
        String stTag            = args[1];

        if(args.length > 2) {
            filter  = args[2];

            selectionFilter = new TestSelectionFilter(filter);
        }

        if(null == stTag || stTag.trim().length() < 1) {
            exitStatus      = 2;

            showUsage(exitStatus, "No selector tag defined!") ;

        }

        StandaloneJsonConfig config = null;

        try {
            config = new StandaloneJsonConfig(jsonConfigFile, args, stTag);
        } catch(Exception e) {
            LOGGER.error("main: Problem parsing the json configuration", e);

            System.err.println("Problems parsing the configuration file: " + e.getLocalizedMessage());

            System.exit(3) ;
        }

        Set<SmokeTestStrategy> jsonTests = new CopyOnWriteArraySet<>();

        int failedCount = 0;
        int passedCount = 0;

        long start      = System.nanoTime();

        try{
            for(StandaloneJsonConfig.JsonTestDefinition testDef : config.testDefinitions) {

                ////////////////////////////////////////////
                // Start the selection process for this line
                boolean selected        = testToBeSelected(stTag, testDef);
                boolean passedFilter    = false;

                if(selected) {
                    //////////////////////////
                    // Matched the environment
                    String cmdLine = testDef.getCmd();

                    if (null != selectionFilter) {
                        //////////////////////////////////////
                        // A command line filter was specified
                        passedFilter = selectionFilter.isMatch(testDef.getId(), cmdLine);
                    } else {
                        passedFilter = true;
                    }

                    /////////////////////////////////////////////////////////////
                    // Finally check if the line passed all checks to be executed
                    if(passedFilter) {
                        jsonTests.add(new JsonTestProcessor(config.setup, testDef));
                    }
                }

                LOGGER.debug(
                        String.format(
                                "main: selected [%-5s], passedFilter [%-5s], id [%4s], envValue [%s], cmd [%s]",
                                selected,
                                passedFilter,
                                testDef.getId(),
                                config.setup.getEnvronmentalVariables(),
                                testDef.getCmd()));
            }

            //////////////////////////////
            // Run the scripts in parallel
            List<SmokeTestResult> results =
                    SmokeTestContext.runSmokeTests(
                            jsonTests, threadPoolSize, timeoutSeconds);

            failedCount = processResults(results);
            passedCount = results.size() - failedCount;

            ////////////////////////////////////////////////
            // Indicate if there was a failure to the caller
            if(failedCount > 0) {
                exitStatus = 5;
            }
        } catch (SmokeTestException e) {
            System.err.println(e);
            exitStatus = 3;
        }

        processSummaryAndExit(LOGGER, exitStatus, failedCount, passedCount, start);
    }


    private static void showUsage(final int exitStatus, final String errMsg) {
        StringBuilder usage = new StringBuilder();

        usage.append("Usage for JsonConfigProcessor - v" + VERSION + "\n");
        usage.append("\n");
        usage.append("JsonConfigProcessor config selector_tag {filter}\n");
        usage.append("   config       = a JSON file containing commands to execute.\n");
        usage.append("   selector_tag = a tag (case insensitive) to select specific lines from the config file to execute.\n");
        usage.append("   filter       = optional pattern to (possibly) reduce the cmd lines selected for execution.\n");
        usage.append("\n");
        usage.append("Notes:\n");
        usage.append("   the filter is of the format (1st characters denotes the type of filter):-\n");
        usage.append("      " + TestSelectionFilter.REGEX_FILTER_PREFIX + "cmd line contains this regex\n");
        usage.append("      " + TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED + "cmd line does NOT contain this regex\n");
        usage.append("      " + TestSelectionFilter.PLAIN_FILTER_PREFIX + "cmd line contains this text\n");
        usage.append("      " + TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED + "cmd line does NOT contain this text\n");
        usage.append("      " + TestSelectionFilter.TEST_ID_SENTINAL + "match this(these) id('s)" + TestSelectionFilter.TEST_ID_SENTINAL + "\n");
        usage.append("\n");
        usage.append("   the filters are only applied if the line has ALREADY been selected by matching the selector tag.\n");
        usage.append("   the id filter MUST end with a sentinal " +  TestSelectionFilter.TEST_ID_SENTINAL + ".\n");
        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("   JsonConfigProcessor scripts.json dev\n");
        usage.append("   JsonConfigProcessor scripts.json UAT\n");
        usage.append("   JsonConfigProcessor scripts.json sit  " + TestSelectionFilter.PLAIN_FILTER_PREFIX + "jsp\n");
        usage.append("   JsonConfigProcessor scripts.json PROD " + TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED + "admin\n");
        usage.append("   JsonConfigProcessor scripts.json sit  " + TestSelectionFilter.REGEX_FILTER_PREFIX + "a.+[\\d]{3}\n");
        usage.append("   JsonConfigProcessor scripts.json QA " + TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED + "A.+Servlet.*\n");

        usage.append("   JsonConfigProcessor scripts.json qa   "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "id1"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        usage.append("   JsonConfigProcessor scripts.json uat  "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "id2"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "id3"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        String usageMsg    =   String.format("%s\n\n%s", errMsg, usage.toString());

        System.err.println(usageMsg);

        LOGGER.warn("showUsage: " + errMsg);

        doExit(LOGGER, exitStatus);
    }

    /**
     * Determine if the test can be selected for execution
     *
     * @param tag
     * @param testDefinition The test definition
     *
     * @return true if the test can be executed; else false
     */
    protected static boolean testToBeSelected(
            final String tag,
            final StandaloneJsonConfig.JsonTestDefinition testDefinition) {

        //////////////////
        // Easy ones first
        if (StandaloneJsonConfig.JsonTestDefinition.RUN.NEVER  == testDefinition.getRun()) return false;
        if (StandaloneJsonConfig.JsonTestDefinition.RUN.ALWAYS == testDefinition.getRun()) return true;

        ///////////////////////////////
        // Hm, we have an IF or UNLESS
        //
        ///////////////////////////////

        //////////////////////////////
        // check the UNLESS ones first
        if(StandaloneJsonConfig.JsonTestDefinition.RUN.UNLESS_TAG_MATCHES == testDefinition.getRun()) {
            for(String runTag : testDefinition.getRunTags())  {
                if(tag.equalsIgnoreCase(runTag)) {
                    return false;
                }
            }
        }

        /////////////////////////
        // check the IF ones now
        if(StandaloneJsonConfig.JsonTestDefinition.RUN.IF_TAG_MATCHES == testDefinition.getRun()) {
            for(String runTag : testDefinition.getRunTags())  {
                if(tag.equalsIgnoreCase(runTag)) {
                    return true;
                }
            }
        }

        return false;
    }
}
