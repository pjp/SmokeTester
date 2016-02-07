package com.pearceful.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Paul Pearce on 2016-01-04.
 *
 * Read a file that contains commands or shell scripts (one per line) to execute
 * in parallel.
 *
 * Example file is scripts-dos.txt which contains for information about how
 * to configure what to run.
 *
 */
public class ShellScriptListProcessor {
    public static final String COMMENT_LEADER       = "#";
    public static final String TAG_SENTINAL         = ":";
    public static final String SELECTED_PREFIX      = "+";
    public static final String NOT_SELECTED_PREFIX  = "-";
    public static final String VALUE_SENTINAL       = "=";

    public static final String TAG_ENV_NAME     = "st.env";
    public static final String TAG_ENV_VALUE    = "st.value";

    private static final Logger LOGGER                  = Logger.getLogger(ShellScriptListProcessor.class);

    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 10;   // Should be configurable
        int timeoutSeconds  = 600;  // Should be configurable
        String filter       = null;
        LineFilter lineFilter   =   null;

        if(args.length < 2) {
            exitStatus      = 1;
            String errMsg   = "Need a file name to read as input and an environment selector tag!";

            System.err.println(errMsg);
            LOGGER.warn("main: " + errMsg);

            LOGGER.info(String.format("main: exiting with [%d]", exitStatus));

            System.exit(exitStatus);
        }

        Path path       = Paths.get(args[0]);
        String stEnv    = args[1];

        if(args.length > 2) {
            filter  = args[2];

            lineFilter = new LineFilter(filter);
        }

        if(null == stEnv || stEnv.trim().length() < 1) {
            exitStatus      = 2;
            String errMsg   = "No environment selector tag defined!";

            System.err.println(errMsg);

            LOGGER.warn("main: " + errMsg);

            LOGGER.info(String.format("main: exiting with [%d]", exitStatus));

            System.exit(exitStatus);
        }

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        String envValueToBeSet  =    null;

        /////////////////////////////////////////
        // Process each non comment or blank line
        LOGGER.debug(
                String.format("main: input [%s], env tag [%s], filter [%s]",
                        path.getFileName(),
                        stEnv,
                        (null != lineFilter ? filter : "{NONE-SPECIFIED}")));

        int failedCount = 0;
        int passedCount = 0;
        long start      = System.nanoTime();

        try{
            List<String> lines  = Files.readAllLines(path);
            int lineNumber      = 0;

            for(String line : lines) {
                lineNumber++;

                ////////////////////////////////////////////////////////////////////
                // Determine if a matching variable for the environment has been set
                String possibleEnvValueToBeSet  = valueToBeSelected(lineNumber, line, stEnv);
                if(null != possibleEnvValueToBeSet) {
                    envValueToBeSet = possibleEnvValueToBeSet;
                }

                ////////////////////////////////////////////
                // Start the selection process for this line
                boolean selected        = lineToBeSelected(lineNumber, line, stEnv);
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
                        shellScripts.add(new ShellScriptProcessor(lineNumber, cmdLine, stEnv, envValueToBeSet));
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

        long elapsed = (System.nanoTime() - start) / 1000000;

        //////////
        // Summary
        String summary = String.format(
                "SUMM: %d pass(es) - %d failure(s) - elapsed %d mS",
                passedCount,
                failedCount,
                elapsed);

        LOGGER.info(String.format("main: %s - exiting with [%d]\n", summary, exitStatus));

        System.out.println(summary);

        System.out.printf("EXIT: %d\n", exitStatus);

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

    protected static String valueToBeSelected(final int lineNumber, final String line, final String tag) {
        if(null == line)        return null;

        if(null == tag)         return null;

        if(line.length() < 5)   return null;

        if (line.startsWith(COMMENT_LEADER + VALUE_SENTINAL)) {
            ////////////////////////////////////////
            // Extract the first token from the line
            StringTokenizer st = new StringTokenizer(line.substring(2), VALUE_SENTINAL);

            if (st.countTokens() < 2) return null;

            // Extract the environment tag and it's value
            String stEnv = st.nextToken();
            String value = line.substring(stEnv.length() + 1);

            if (tag.toLowerCase().equals(stEnv.toLowerCase())) return value.substring(2);
        }

        return null;
    }

    static class LineFilter {
        public static final String REGEX_FILTER_PREFIX          =   "==";
        public static final String REGEX_FILTER_PREFIX_INVERTED =   "=!";
        public static final String PLAIN_FILTER_PREFIX          =   "~=";
        public static final String PLAIN_FILTER_PREFIX_INVERTED =   "~!";
        public static final String LINE_NUMBER_PREFIX           =   "#";

        Pattern regex            = null;
        String rawFilterText     = null;
        String plainFilterText   = null;
        boolean invertMatch      = false;
        boolean lineNumberMatch  = false;

        public LineFilter(final String rawFilterText) {
            this.rawFilterText  =   rawFilterText;

            setUp(rawFilterText);
        }

        public boolean isMatch(final int lineNumber, final String text) {
            boolean matched =   false;

            if(null != regex) {
                Matcher matcher =   regex.matcher(text);
                matched         =   matcher.find();
            } else {
                if(lineNumberMatch) {
                    matched = plainFilterText.contains(
                            String.format(
                                    "%s%d%s",
                                    LINE_NUMBER_PREFIX,
                                    lineNumber,
                                    LINE_NUMBER_PREFIX));
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

            if(filterText.startsWith(LINE_NUMBER_PREFIX)) {
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
