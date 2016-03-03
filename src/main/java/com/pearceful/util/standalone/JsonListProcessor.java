package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;
import com.pearceful.util.SmokeTestException;
import com.pearceful.util.SmokeTestResult;
import com.pearceful.util.SmokeTestStrategy;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Paul Pearce on 2016-01-04.
 *
 * Read a file that contains commands to execute in parallel.
 *
 * Example file is sample-conf.json which contains for information about how
 * to configure and what to run.
 *
 */
public class JsonListProcessor {
    public static final String VERSION              = "1.0";

    private static final Logger LOGGER                  = Logger.getLogger(JsonListProcessor.class);

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

            ///////////////////////////////////
            // Display the result for each test
            for(SmokeTestResult result : results) {
                if(result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                System.out.println(result.getMessage());
            }

            /////////////////////////////////
            // Determine the slowest commands
            List<SmokeTestResult> slowestResults = new ArrayList<>() ;
            slowestResults.addAll(results);

            Collections.sort(slowestResults, new Comparator<SmokeTestResult>() {
                @Override
                public int compare(SmokeTestResult o1, SmokeTestResult o2) {
                    int result = 0;

                    if (o1.getElapsedNanoSeconds() > o2.getElapsedNanoSeconds()) {
                        result = -1;
                    } else if (o1.getElapsedNanoSeconds() < o2.getElapsedNanoSeconds()) {
                       result = 1;
                    }

                    return result;
                }
            });

            //////////////////////////
            // Display the top slowest
            int max = 5;
            System.out.println("COMM: ################################################");
            System.out.println("SUMM: Top " + max + " Slowest (PASS) responses follow.");

            int count = 1;
            for(SmokeTestResult result : slowestResults) {
                if (count > max) {
                    break;
                }

                if(result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
                    System.out.println(result.getMessage());
                    count++ ;
                }
            }

            ////////////////////////////
            // Display just the failures
            System.out.println("COMM: ################################################");
            System.out.println("SUMM: All (FAIL) responses follow.");

            for(SmokeTestResult result : results) {
                if(! result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
                    System.out.println(result.getMessage());
                }
            }

            ////////////////////////////////////////////////
            // Indicate if there was a failure to the caller
            if(failedCount > 0) {
                exitStatus = 5;
            }
        } catch (SmokeTestException e) {
            System.err.println(e);
            exitStatus = 3;
        }

        long elapsed = (System.nanoTime() - start) / 1000000;

        //////////
        // Summary
        String summary = String.format(
                "SUMM: %d pass(es) - %d failure(s) - elapsed %d mS",
                passedCount,
                failedCount,
                elapsed);

        LOGGER.info(String.format("main: %s - exiting with [%d]\n", summary, exitStatus));

        System.out.println("COMM: ################################################");
        System.out.println(summary);

        doExit(exitStatus);
    }

    private static void showUsage(final int exitStatus, final String errMsg) {
        StringBuilder usage = new StringBuilder();

        usage.append("Usage for JsonListProcessor - v" + VERSION + "\n");
        usage.append("\n");
        usage.append("JsonListProcessor config selector_tag {filter}\n");
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
        usage.append("      " + TestSelectionFilter.TEST_ID_SENTINAL + "match this(these) line number(s)" + TestSelectionFilter.TEST_ID_SENTINAL + "\n");
        usage.append("\n");
        usage.append("   the filters are only applied if the line has ALREADY been selected by matching the selector tag.\n");
        usage.append("   the line number filter MUST end with a sentinal " +  TestSelectionFilter.TEST_ID_SENTINAL + ".\n");
        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("   JsonListProcessor scripts.json dev\n");
        usage.append("   JsonListProcessor scripts.json UAT\n");
        usage.append("   JsonListProcessor scripts.json sit  " + TestSelectionFilter.PLAIN_FILTER_PREFIX + "jsp\n");
        usage.append("   JsonListProcessor scripts.json PROD " + TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED + "admin\n");
        usage.append("   JsonListProcessor scripts.json sit  " + TestSelectionFilter.REGEX_FILTER_PREFIX + "a.+[\\d]{3}\n");
        usage.append("   JsonListProcessor scripts.json QA " + TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED + "A.+Servlet.*\n");

        usage.append("   JsonListProcessor scripts.json qa   "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "37"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        usage.append("   JsonListProcessor scripts.json uat  "
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "39"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "47"
                + TestSelectionFilter.TEST_ID_SENTINAL
                + "\n");

        String usageMsg    =   String.format("%s\n\n%s", errMsg, usage.toString());

        System.err.println(usageMsg);

        LOGGER.warn("showUsage: " + errMsg);

        doExit(exitStatus);
    }

    private static void doExit(final int exitStatus) {
        String exitMsg = String.format("EXIT: %d\n", exitStatus);

        LOGGER.info(exitMsg);

        System.out.println("COMM: ################################################");

        System.out.println(exitMsg);

        System.exit(exitStatus);
    }

    /**
     *
     * @param line
     * @return
     */
    protected static String stripLeadingToken(final String line) {
        String restOfLine   = "";

        if(null == line) return "";

        int lastOffset      = line.length() - 1;


        int index = line.indexOf(" ");

        if(-1 == index) {
            index = line.indexOf("\t");
        }

        if(index >= 0) {
            index++;

            if (index <= lastOffset) {
                restOfLine = line.substring(index);
            }
        }

        return restOfLine;
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

    protected static boolean onWindows() {
        boolean onWindows = false;

        ////////////////////////////////////////////////////////
        // Quick and dirty test for determining the shell to use
        String osName = System.getProperty("os.name");
        osName = osName.toLowerCase(Locale.ENGLISH);

        if (osName.indexOf("windows") != -1) {
            onWindows = true;
        }

        return onWindows;
    }

    static class TestSelectionFilter {
        public static final String REGEX_FILTER_PREFIX          =   "==";
        public static final String REGEX_FILTER_PREFIX_INVERTED =   "=!";
        public static final String PLAIN_FILTER_PREFIX          =   "~=";
        public static final String PLAIN_FILTER_PREFIX_INVERTED =   "~!";
        public static final String TEST_ID_SENTINAL             =   "#";

        Pattern regex            = null;
        String rawFilterText     = null;
        String plainFilterText   = null;
        boolean invertMatch      = false;
        boolean lineNumberMatch  = false;

        public TestSelectionFilter(final String rawFilterText) {
            this.rawFilterText  =   rawFilterText;

            setUp(rawFilterText);
        }

        public boolean isMatch(final String id, final String text) {
            boolean matched =   false;

            if(null != regex) {
                Matcher matcher =   regex.matcher(text);
                matched         =   matcher.find();
            } else {
                if(lineNumberMatch) {
                    matched = plainFilterText.contains(
                            String.format(
                                    "%s%s%s",
                                    TEST_ID_SENTINAL,
                                    id,
                                    TEST_ID_SENTINAL));
                } else {
                    matched = text.contains(plainFilterText);
                }
            }

            if(invertMatch) {
                matched = ! matched;
            }

            return matched;
        }

        protected void setUp(final String filterText) {
            if(null == filterText || filterText.length() < 1) {
                throw new IllegalArgumentException("Null or empty filter text specified");
            }

            rawFilterText = filterText;

            if(filterText.startsWith(TEST_ID_SENTINAL)) {
                plainFilterText = filterText;
                lineNumberMatch = true;

            } else if(filterText.startsWith(REGEX_FILTER_PREFIX)) {
                regex = Pattern.compile(filterText.substring(REGEX_FILTER_PREFIX.length()));

            } else if(filterText.startsWith(REGEX_FILTER_PREFIX_INVERTED)) {
                regex = Pattern.compile(filterText.substring(REGEX_FILTER_PREFIX_INVERTED.length()));
                invertMatch =   true;

            } else if(filterText.startsWith(PLAIN_FILTER_PREFIX)) {
                if(filterText.length() < 3) {
                    throw new IllegalArgumentException(
                            "Invalid filter text specified [" + filterText + "]");
                }
                plainFilterText = filterText.substring(PLAIN_FILTER_PREFIX.length());

            } else if(filterText.startsWith(PLAIN_FILTER_PREFIX_INVERTED)) {
                if(filterText.length() < 3) {
                    throw new IllegalArgumentException(
                            "Invalid filter text specified [" + filterText + "]");
                }
                plainFilterText = filterText.substring(PLAIN_FILTER_PREFIX_INVERTED.length());
                invertMatch =   true;

            } else {
                if(filterText.startsWith(PLAIN_FILTER_PREFIX)) {
                    plainFilterText = filterText.substring(PLAIN_FILTER_PREFIX.length());
                } else {
                    ///////////////////////////
                    // The default if no prefix
                    plainFilterText = filterText;
                }

                if(plainFilterText.length() < 1) {
                    throw new IllegalArgumentException(
                            "Invalid filter text specified [" + filterText + "]");
                }
            }
        }
    }
}
