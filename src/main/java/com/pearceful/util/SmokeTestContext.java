package com.pearceful.util;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by pjp on 2015-12-26.
 *
 * A generic Smoke test harness using the Strategy pattern, the actual test is encapsulated as a Strategy, see
 * the SmokeTestStrategy interface and BaseSmokeTestStrategy abstract class.
 */
public class SmokeTestContext {
    private static final Logger LOGGER                  = Logger.getLogger(SmokeTestContext.class);

    /**
     * Run the smoke tests (multi-threaded)
     *
     * @param smokeTestStrategies A set of smoke test strategies to be run
     * @param threadPoolSize The thread pool size to run the test in
     * @param timeoutInSeconds How long to wait for all the tests to complete
     *
     * @return A list of results from each smoke test.
     *
     * @throws SmokeTestException
     */
    public static List<SmokeTestResult> runSmokeTests(
            final Set<SmokeTestStrategy> smokeTestStrategies,
            final int threadPoolSize,
            final long timeoutInSeconds) throws SmokeTestException {

        ////////////////
        // Sanity checks
        if(null == smokeTestStrategies) {
            throw new IllegalArgumentException("runSmokeTests: smokeTestStrategies list cannot be null");
        }

        if(threadPoolSize < 1) {
            throw new IllegalArgumentException("runSmokeTests: threadPoolSize must > 0");
        }

        if(timeoutInSeconds < 1) {
            throw new IllegalArgumentException("runSmokeTests: timeoutInSeconds must be > 0");
        }

        LOGGER.info(String.format("runSmokeTests: Processing %d test(s)", smokeTestStrategies.size()));

        ////////////////////////////
        // Build a list of Callables
        List<Callable<SmokeTestResult>> smokeTestCallables  = new ArrayList<Callable<SmokeTestResult>>();

        /////////////////////////////////
        // Build an empty list of results
        List<SmokeTestResult> smokeTestResults              = new ArrayList<SmokeTestResult>();

        ////////////////////////////////////////////////
        // Create the (possibly multi-threaded) executor
        ExecutorService smokeTestExecutor       = Executors.newFixedThreadPool(threadPoolSize);

        try {
            smokeTestStrategies
                    .stream()
                    .map( smokeTestStratgy -> {
                        return new SmokeTestCallable(smokeTestStratgy);
                    })
                    .forEach(smokeTestCallables::add);

            ////////////////////////////////////////
            // Do the work (possibly multi-threaded)
            //
            // What happens if one or more smokeTestCallables fails?, we want to record it's
            // id and carry on with the rest
            //
            smokeTestExecutor.invokeAll(smokeTestCallables, timeoutInSeconds, TimeUnit.SECONDS)
                    .stream()
                    .map( future -> {
                        try {
                            return future.get();
                        } catch (Throwable t) {
                            String msg = future.toString();

                            // Should not get here as SmokeTestCallable.call traps all Throwables, but ..........
                            if(t instanceof CancellationException) {
                                LOGGER.warn("runSmokeTests: Test [" + msg + "] cancelled due to Executor timeout");
                            } else {
                                LOGGER.error("runSmokeTests: Test [" + msg + "] Executor error", t);
                            }

                            return new SmokeTestResult(
                                    msg,  // Can't get the id at this point :-(
                                    SmokeTestResult.STATE.ERROR,
                                    0L,
                                    t.toString());
                        }
                    })
                    .forEach(smokeTestResults::add);
        } catch (InterruptedException e) {
            LOGGER.error("runSmokeTests: Problem executing tests", e);
            throw new SmokeTestException(e);
        } finally {
            smokeTestExecutor.shutdown();
        }

        return smokeTestResults;
    }
}
