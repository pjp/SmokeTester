package com.pearceful.util;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by pjp on 2015-12-26.
 */
public class SmokeTestContext {
    private static final Logger LOGGER                  = Logger.getLogger(SmokeTestContext.class);

    /**
     * Run the smoke tests (multi-threaded)
     *
     * @param smokeTestStrategies A list of smoke test strategies to be run
     * @param threadPoolSize The thread pool size to run the test in
     * @param timeoutInSeconds How long to wait for all the tests to complete
     *
     * @return A list of results from each smoke test.
     *
     * @throws InterruptedException
     */
    public List<SmokeTestResult> runSmokeTests(
            final List<SmokeTestStrategy> smokeTestStrategies,
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
        List<Callable<SmokeTestResult>> smokeTestCallables = new ArrayList<Callable<SmokeTestResult>>();

        smokeTestStrategies
                .stream()
                .map( smokeTestStratgy -> {
                    return new SmokeTestCallable(smokeTestStratgy);
                })
                .forEach(smokeTestCallables::add);

        //////////////////////
        // Create the executor
        ExecutorService smokeTestExecutor       = Executors.newFixedThreadPool(threadPoolSize);
        List<SmokeTestResult> smokeTestResults  = new ArrayList<SmokeTestResult>();

        ///////////////////////////////
        // Do the work (multi-threaded)
        //
        // What happens if one or more smokeTestCallables fails?, we want to record it's
        // id and carry on with the rest
        //
        try {
            smokeTestExecutor.invokeAll(smokeTestCallables, timeoutInSeconds, TimeUnit.SECONDS)
                    .stream()
                    .map( future -> {
                        try {
                            return future.get();
                        } catch (Throwable e) {
                            LOGGER.error("runSmokeTests", e);

                            return new SmokeTestResult(
                                    future.toString(),  // Don't know the id at this point :-(
                                    SmokeTestResult.STATE.ERROR,
                                    0L,
                                    e.toString());
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
