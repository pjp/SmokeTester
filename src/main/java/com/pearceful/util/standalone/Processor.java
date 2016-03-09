package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestResult;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ppearce on 2016-03-08.
 *
 *
 * Common superclass with useful common methods
 */
public abstract class Processor {
    /**
     *
     * @param results
     * @return
     */
    protected static int processResults(List<SmokeTestResult> results) {
        int failedCount = 0;

        ///////////////////////////////////
        // Display the result for each test
        for(SmokeTestResult result : results) {
            if(! result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
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
        return failedCount;
    }

    /**
     *
     * @param LOGGER
     * @param exitStatus
     * @param failedCount
     * @param passedCount
     * @param start
     */
    protected static void processSummaryAndExit(final Logger LOGGER, int exitStatus, int failedCount, int passedCount, long start) {
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

        doExit(LOGGER, exitStatus);
    }

    /**
     *
     * @param LOGGER
     * @param exitStatus
     */
    protected static void doExit(final Logger LOGGER, final int exitStatus) {
        String exitMsg = String.format("EXIT: %d\n", exitStatus);

        LOGGER.info(exitMsg);

        System.out.println("COMM: ################################################");

        System.out.println(exitMsg);

        System.exit(exitStatus);
    }

    /**
     *
     * @return
     */
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
     *
     */
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
        boolean idMatch          = false;

        /**
         *
         * @param rawFilterText
         */
        public TestSelectionFilter(final String rawFilterText) {
            this.rawFilterText  =   rawFilterText;

            setUp(rawFilterText);
        }

        /**
         *
         * @param id
         * @param text
         * @return
         */
        public boolean isMatch(final int id, final String text) {
            return isMatch("" + id, text);
        }

        /**
         *
         * @param id
         * @param text
         * @return
         */
        public boolean isMatch(final String id, final String text) {
            boolean matched =   false;

            if(null != regex) {
                Matcher matcher =   regex.matcher(text);
                matched         =   matcher.find();
            } else {
                if(idMatch) {
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

        /**
         *
         * @param filterText
         */
        protected void setUp(final String filterText) {
            if(null == filterText || filterText.length() < 1) {
                throw new IllegalArgumentException("Null or empty filter text specified");
            }

            rawFilterText = filterText;

            if(filterText.startsWith(TEST_ID_SENTINAL)) {
                plainFilterText = filterText;
                idMatch = true;

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
