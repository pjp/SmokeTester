package com.pearceful.util;

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
 * in parallel. Example file is scripts.txt
 *
 * As per the shell, lines starting with # as ignored
 *
 */
public class ShellScriptListProcessor {
    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 10;
        int timeoutSeconds  = 600;
        String env          = "";

        if(args.length < 1) {
            System.err.println("Need a file name to read as input");
            System.exit(1);
        }

        Path path   = Paths.get(args[0]);
        env         = System.getenv(TAG_ENV_NAME);

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        /////////////////////////////////////////
        // Process each non comment or blank line

        try{
            List<String> lines  = Files.readAllLines(path);
            int lineNumber      = 0;

            for(String line : lines) {
                lineNumber++;

                if(lineToBeSelected(lineNumber, line, env)) {
                    String cmdLine = stripLeadingToken(line);
                    shellScripts.add(new ShellScriptProcessor(lineNumber, cmdLine));
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
                exitStatus = 4;
            }
        } catch (IOException e) {
            System.err.println(e);
            exitStatus = 2;
        } catch (SmokeTestException e) {
            System.err.println(e);
            exitStatus = 3;
        }

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

    public static final String COMMENT_LEADER   = "#";
    public static final String TAG_SENTINAL     = ":";
    public static final String TAG_ENV_NAME     = "ST_ENV";

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

        if(line.startsWith(COMMENT_LEADER + TAG_SENTINAL)) {
            if(line.contains(TAG_SENTINAL + "-" + TAG_SENTINAL)) return false;
            if(line.contains(TAG_SENTINAL + "-" + tag + TAG_SENTINAL)) return false;

            if(line.contains(TAG_SENTINAL + "+" + TAG_SENTINAL)) return true;
            if(line.contains(TAG_SENTINAL + "+" + tag + TAG_SENTINAL)) return true;
        }

        return false;
    }
}
