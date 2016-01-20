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
     * The called flag in each Strategy will be set if it's actually executed, which may not be the
     * case if the ThreadPool actually times out.
     *
     * @param smokeTestStrategies A set of smoke test strategies to be run
     * @param threadPoolSize The thread pool size to run the test in
     * @param timeoutInSeconds How long to wait for all the tests to complete
     *
     * @return A list of results from each smoke test, if an error or timeout from the ThreadPool occurs, then a
     * result will be synthesised for the test.
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
            List<Future<SmokeTestResult>> futures =
                    smokeTestExecutor.invokeAll(smokeTestCallables, timeoutInSeconds, TimeUnit.SECONDS);

            //////////////////////////////////////////////
            // Determine how many Callables were passed in
            int callableSize = smokeTestCallables.size();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Since it's guaranteed that the order of these futures are in the same order as the list of callables
            // the id can be determined
            for(int i = 0 ; i < callableSize ; i++) {
                SmokeTestCallable callable      = (SmokeTestCallable)smokeTestCallables.get(i);
                SmokeTestStrategy strategy      = callable.getSmokeTestStrategy();
                String strategyId               = strategy.getId();
                SmokeTestResult smokeTestResult;

                try {
                    Future<SmokeTestResult> future = futures.get(i);

                    /////////////////////////////////
                    // Get the result from the future
                    smokeTestResult = future.get();

                    ////////////////
                    // Sanity checks
                    if(! strategyId.equals(smokeTestResult.getId())) {
                        String errMsg =
                                "runSmokeTests: result's id ["
                                + smokeTestResult.getId()
                                + "] does not match expected id ["
                                + strategyId
                                + "]";

                        throw new SmokeTestException(errMsg);
                    }
                } catch(Throwable t) {
                    if (t instanceof CancellationException) {
                        LOGGER.warn("runSmokeTests: Test [" + strategyId + "] cancelled due to Executor timeout", t);
                    } else {
                        LOGGER.error("runSmokeTests: Test [" + strategyId + "] Executor error", t);
                    }

                    smokeTestResult = new SmokeTestResult(
                            strategyId,
                            SmokeTestResult.STATE.ERROR,
                            0L,
                            t.toString());
                }

                smokeTestResults.add(smokeTestResult);
            }
        } catch (InterruptedException e) {
            LOGGER.error("runSmokeTests: Problem executing tests", e);
            throw new SmokeTestException(e);
        } finally {
            smokeTestExecutor.shutdown();
        }

        return smokeTestResults;
    }
}
