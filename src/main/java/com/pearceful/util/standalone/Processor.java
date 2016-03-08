package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestResult;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by ppearce on 2016-03-08.
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


}
