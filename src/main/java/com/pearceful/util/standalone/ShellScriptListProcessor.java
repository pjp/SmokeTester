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
    public static final String GLOBAL_SENTINAL      = "@";

    public static final String ENV_VARIABLE_NAME_PREFIX_KEY = "st.env.name.prefix";
    public static final String ENV_VARIABLE_NAME_PREFIX     = "ST_";
    public static final String ENV_VARIABLE_ENV_SUFFIX      = "ENV";
    public static final String ENV_VARIABLE_VALUE_SUFFIX    = "VALUE";
    public static final String ENV_VARIABLE_OS_SUFFIX       = "OS";
    public static final String ENV_VARIABLE_LINE_SUFFIX     = "LINE";

    public static final String TIMEOUT_SECONDS  = "TIMEOUT_SECONDS";
    public static final String THREAD_POOL_SIZE = "THREAD_POOL_SIZE";
    public static final int INT_VALUE_NOT_SET   = -1;

    private static final Logger LOGGER                  = Logger.getLogger(ShellScriptListProcessor.class);
    private static String envVariableNamePrefix         = ENV_VARIABLE_NAME_PREFIX;

    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 5;
        int timeoutSeconds  = 600;
        String filter       = null;
        LineFilter lineFilter   =   null;

        if(args.length < 2) {
            exitStatus      = 1;

            showUsage(exitStatus, "Need a file name to read as input and an environment selector tag!");
        }

        Path path       = Paths.get(args[0]);
        String stTag    = args[1];

        if(args.length > 2) {
            filter  = args[2];

            lineFilter = new LineFilter(filter);
        }

        if(null == stTag || stTag.trim().length() < 1) {
            exitStatus      = 2;

            showUsage(exitStatus, "No selector tag defined!") ;

        }

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        String envValueToBeSet  =    null;

        /////////////////////////////////////////
        // Process each non comment or blank line
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
                        buildEnvVariableName(ENV_VARIABLE_ENV_SUFFIX),
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
                                new ShellScriptProcessor(lineNumber, cmdLine, stTag, envValueToBeSet));
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

        doExit(exitStatus);
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

        usage.append("Usage for ShellScriptListProcessor\n");
        usage.append("\n");
        usage.append("ShellScriptListProcessor config env {filter}\n");
        usage.append("   config = a file containing commands to execute.\n");
        usage.append("   env    = a tag to select specific lines from the config to execute.\n");
        usage.append("   filter = optional pattern to (possibly) reduce the lines selected for execution.\n");
        usage.append("\n");
        usage.append("Notes:\n");
        usage.append("   filter is of the format (1st characters denote the type of filter):-\n");
        usage.append("      " + LineFilter.REGEX_FILTER_PREFIX + "match this regex\n");
        usage.append("      " + LineFilter.REGEX_FILTER_PREFIX_INVERTED + "dont match this regex\n");
        usage.append("      " + LineFilter.PLAIN_FILTER_PREFIX + "contains this text\n");
        usage.append("      " + LineFilter.PLAIN_FILTER_PREFIX_INVERTED + "dont contain this text\n");
        usage.append("      " + LineFilter.LINE_NUMBER_SENTINAL + "match this line number" + LineFilter.LINE_NUMBER_SENTINAL + "\n");
        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("   ShellScriptListProcessor scripts.txt dev\n");
        usage.append("   ShellScriptListProcessor scripts.txt UAT\n");
        usage.append("   ShellScriptListProcessor scripts.txt sit  " + LineFilter.PLAIN_FILTER_PREFIX + "jsp\n");
        usage.append("   ShellScriptListProcessor scripts.txt PROD " + LineFilter.PLAIN_FILTER_PREFIX_INVERTED + "admin\n");
        usage.append("   ShellScriptListProcessor scripts.txt sit  " + LineFilter.REGEX_FILTER_PREFIX + "a.+[\\d]{3}\n");
        usage.append("   ShellScriptListProcessor scripts.txt PROD " + LineFilter.REGEX_FILTER_PREFIX_INVERTED + "A.+Servlet.*\n");

        usage.append("   ShellScriptListProcessor scripts.txt qa   "
                + LineFilter.LINE_NUMBER_SENTINAL
                + "37"
                + LineFilter.LINE_NUMBER_SENTINAL
                + "\n");

        usage.append("   ShellScriptListProcessor scripts.txt uat  "
                + LineFilter.LINE_NUMBER_SENTINAL
                + "39"
                + LineFilter.LINE_NUMBER_SENTINAL
                + "47"
                + LineFilter.LINE_NUMBER_SENTINAL
                + "\n");

        String usageMsg    =   String.format("%s\n\n%s", errMsg, usage.toString());

        System.err.println(usageMsg);

        LOGGER.warn("showUsage: " + errMsg);

        doExit(exitStatus);
    }

    private static void doExit(final int exitStatus) {
        String exitMsg = String.format("EXIT: %d\n", exitStatus);

        LOGGER.info(exitMsg);

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
     * @param suffix
     * @return
     */
    protected static String buildEnvVariableName(final String suffix) {
        return envVariableNamePrefix + suffix;
    }

    static class LineFilter {
        public static final String REGEX_FILTER_PREFIX          =   "==";
        public static final String REGEX_FILTER_PREFIX_INVERTED =   "=!";
        public static final String PLAIN_FILTER_PREFIX          =   "~=";
        public static final String PLAIN_FILTER_PREFIX_INVERTED =   "~!";
        public static final String LINE_NUMBER_SENTINAL         =   "#";

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
                                    LINE_NUMBER_SENTINAL,
                                    lineNumber,
                                    LINE_NUMBER_SENTINAL));
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

            if(filterText.startsWith(LINE_NUMBER_SENTINAL)) {
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
