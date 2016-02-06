package com.pearceful.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by ppearce on 2016-02-04.
 *
 * Read a file that contains commands or shell scripts (one per line) to execute
 * in parallel.
 *
 * Example file is scripts.txt which contains for information about how
 * to configure what to run.
 *
 */
public class ShellScriptListProcessor {
    public static final String COMMENT_LEADER       = "#";
    public static final String TAG_SENTINAL         = ":";
    public static final String SELECTED_PREFIX      = "+";
    public static final String NOT_SELECTED_PREFIX  = "-";

    public static final String TAG_ENV_NAME     = "st.env";
    public static final String TAG_ENV_VALUE    = "st.value";

    private static final Logger LOGGER                  = Logger.getLogger(ShellScriptListProcessor.class);

    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 10;   // Should be configurable
        int timeoutSeconds  = 600;  // Should be configurable

        if(args.length < 1) {
            exitStatus      = 1;
            String errMsg   = "Need a file name to read as input!";

            System.err.println(errMsg);
            LOGGER.warn("main: " + errMsg);

            LOGGER.info(String.format("main: exiting with [%d]", exitStatus));

            System.exit(exitStatus);
        }

        Path path       = Paths.get(args[0]);

        ////////////
        // Mandatory
        String stEnv    = System.getProperty(TAG_ENV_NAME);

        if(null == stEnv) {
            exitStatus      = 2;
            String errMsg   = "No system property " + TAG_ENV_NAME + " defined!";

            System.err.println(errMsg);

            LOGGER.warn("main: " + errMsg);

            LOGGER.info(String.format("main: exiting with [%d]", exitStatus));

            System.exit(exitStatus);
        }

        // Optional
        String stVal                        = System.getProperty(TAG_ENV_VALUE);

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        /////////////////////////////////////////
        // Process each non comment or blank line
        LOGGER.debug(String.format("main: env [%s]", stEnv));

        try{
            List<String> lines  = Files.readAllLines(path);
            int lineNumber      = 0;

            for(String line : lines) {
                lineNumber++;

                boolean selected = lineToBeSelected(lineNumber, line, stEnv);

                LOGGER.debug(String.format("main: selected [%-5s], line [%4d], cmd [%s]", selected, lineNumber, line));

                if(selected) {
                    String cmdLine = stripLeadingToken(line);
                    shellScripts.add(new ShellScriptProcessor(lineNumber, cmdLine, stEnv, stVal));
                }
            }

            //////////////////////////////
            // Run the scripts in parallel
            List<SmokeTestResult> results =
                    SmokeTestContext.runSmokeTests(
                            shellScripts, threadPoolSize, timeoutSeconds);

            ///////////////////////////////////
            // Display the result for each test
            int failedCount = 0;
            int passedCount = 0;

            for(SmokeTestResult result : results) {
                if(result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                System.out.println(result.getMessage());
            }

            //////////
            // Summary
            System.out.printf("SUMMARY: There were %d pass(es) and %d failure(s)", passedCount, failedCount);

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

        LOGGER.info(String.format("main: exiting with [%d]", exitStatus));

        System.exit(exitStatus);
    }

    /**
     *
     * @param line
     * @return
     */
    public static String stripLeadingToken(final String line) {
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
    public static boolean lineToBeSelected(final int lineNumber, final String line, final String tag) {
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
}
