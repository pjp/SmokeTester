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
public abstract class ConfigProcessor {
    public static final String COMMENT_LEADER       = "#";
    public static final String TAG_SENTINAL         = ":";
    public static final String SELECTED_PREFIX      = "+";
    public static final String NOT_SELECTED_PREFIX  = "-";
    public static final String VALUE_SENTINAL       = "=";
    public static final String GLOBAL_SENTINAL      = "@";

    public static final String UNIX_SHELL           = "bash";
    public static final String UNIX_SHELL_PARAM     = "-c";

    public static final String WINDOWS_SHELL        = "cmd";
    public static final String WINDOWS_SHELL_PARAM  = "/c";


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
     * Determine if the line can be selected for execution
     *
     * @param lineNumber
     * @param line The line from the input file
     * @param tag
     *
     * @return true if the line can be executed; else false
     */
    protected static boolean lineToBeSelected(final int lineNumber, final String line, final String tag) {
        if(null == line)        return false;

        if(null == tag)         return false;

        if(line.length() < 2)   return false;

        ////////////////////////////////////////
        // Extract the first token from the line
        StringTokenizer st  = new StringTokenizer(line.toLowerCase());
        if(st.hasMoreTokens()) {
            String token = st.nextToken();

            if (token.startsWith(COMMENT_LEADER + TAG_SENTINAL)) {
                //////////////////
                // Easy ones first
                if (token.contains(TAG_SENTINAL + NOT_SELECTED_PREFIX + TAG_SENTINAL)) return false;
                if (token.contains(TAG_SENTINAL + NOT_SELECTED_PREFIX + tag.toLowerCase() + TAG_SENTINAL)) return false;

                if (token.contains(TAG_SENTINAL + SELECTED_PREFIX + TAG_SENTINAL)) return true;
                if (token.contains(TAG_SENTINAL + tag.toLowerCase() + TAG_SENTINAL)) return true;
                if (token.contains(TAG_SENTINAL + SELECTED_PREFIX + tag + TAG_SENTINAL)) return true;

                ///////////////////////////////////////
                // Hm, we have a combination of + and -
                //
                // build two lists, selected and not selected
                List<String> selected = new ArrayList<>();
                List<String> notSelected = new ArrayList<>();

                StringTokenizer stSelected = new StringTokenizer(token.substring(1), ":");
                while(stSelected.hasMoreTokens()) {
                    String stToken = stSelected.nextToken();
                    if(stToken.startsWith(NOT_SELECTED_PREFIX)) {
                        notSelected.add(stToken.substring(1));
                    } else {
                        if(stToken.startsWith(SELECTED_PREFIX)) {
                            selected.add(stToken.substring(1));
                        } else {
                            selected.add(stToken);
                        }
                    }
                }

                if(! selected.isEmpty()) {
                    if(selected.contains(tag.toLowerCase())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    protected static String valueToBeSelected(final String line, final String tag) {
        if(null == line)        return null;

        if(null == tag)         return null;

        if(line.length() < 5)   return null;

        if (line.startsWith(COMMENT_LEADER + VALUE_SENTINAL)) {
            ////////////////////////////////////////
            // Extract the first token from the line
            StringTokenizer st = new StringTokenizer(line.substring(2), VALUE_SENTINAL);

            if (st.countTokens() < 2) return null;

            // Extract the tag and it's value
            String stEnv = st.nextToken();
            String value = line.substring(stEnv.length() + 1);

            if (tag.toLowerCase().equals(stEnv.toLowerCase())) return value.substring(2);
        }

        return null;
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
