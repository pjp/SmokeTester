package com.pearceful.util;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

/**
 * Created by pjp on 2015-12-26.
 */
public class SmokeTestContextTest extends TestCase {
    public void testGoodSimpleStrategy() throws InterruptedException, SmokeTestException {
        String id                   = "1";
        String msg                  = "message1";
        int threadPoolSize          = 1;
        int timeoutInSeconds        = 2;
        SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_PASS;
        SmokeTestResult mockSmokeTestResult = new SmokeTestResult(id, state, 0, msg);

        // Build a strategy smoke tests that will run in less that the
        // timeout for the overall test to complete in
        MockSimpleBaseSmokeTestStrategy mockSimpleBaseSmokeTestStrategy =
                new MockSimpleBaseSmokeTestStrategy(id, mockSmokeTestResult);

        Set<SmokeTestStrategy> smokeTestStrategies = new HashSet<>();
        smokeTestStrategies.add(mockSimpleBaseSmokeTestStrategy);

        //////////////////////////////////////////////////////////////
        // Execute the strategy smoke test(s) and gather the result(s)
        List<SmokeTestResult> smokeTestResults =
                SmokeTestContext.runSmokeTests(smokeTestStrategies, threadPoolSize, timeoutInSeconds);

        assertNotNull(smokeTestResults);
        assertEquals(1, smokeTestResults.size());

        SmokeTestResult result = smokeTestResults.get(0);

        assertEquals(id, result.getId());
        assertEquals(state, result.getState());
    }

    public void testSimpleStrategyWithForcedException() throws SmokeTestException {
        int timeout = 1;
        String id = "1";
        String msg  = "forcedException";
        SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_ERROR;
        SmokeTestResult mockSmokeTestResult = new SmokeTestResult(id, state, 0, msg);

        // Build a strategy smoke test that will throw an exception
        MockSimpleBaseSmokeTestStrategy mockSimpleBaseSmokeTestStrategy =
                new MockSimpleBaseSmokeTestStrategy(id, mockSmokeTestResult, true, timeout);

        Set<SmokeTestStrategy> smokeTestStrategies = new HashSet<>();
        smokeTestStrategies.add(mockSimpleBaseSmokeTestStrategy);

        List<SmokeTestResult> results = SmokeTestContext.runSmokeTests(smokeTestStrategies, 1, timeout);
        SmokeTestResult retrievedSmokeTestResult = results.get(0);
        assertEquals(SmokeTestResult.STATE.USER_ERROR, retrievedSmokeTestResult.getState());
        assertEquals(msg, retrievedSmokeTestResult.getMessage());
    }

    public void testSimpleStrategyWithTimeout() throws SmokeTestException {
        int timeout = 1;
        String id = "1";
        String msg  = "forcedException";
        SmokeTestResult.STATE state = SmokeTestResult.STATE.USER_ERROR;
        SmokeTestResult mockSmokeTestResult = new SmokeTestResult(id, state, 0, msg);

        // Build a strategy smoke test that will run longer that the
        // timeout for the overall test to complete in
        MockSimpleBaseSmokeTestStrategy mockSimpleBaseSmokeTestStrategy =
                new MockSimpleBaseSmokeTestStrategy(id, mockSmokeTestResult, false, timeout * 2);

        Set<SmokeTestStrategy> smokeTestStrategies = new HashSet<>();
        smokeTestStrategies.add(mockSimpleBaseSmokeTestStrategy);

        List<SmokeTestResult> results = SmokeTestContext.runSmokeTests(smokeTestStrategies, 1, timeout);
        SmokeTestResult retrievedSmokeTestResult = results.get(0);
        assertEquals(SmokeTestResult.STATE.EXEC_ERROR, retrievedSmokeTestResult.getState());
        assertTrue(retrievedSmokeTestResult.getMessage().contains("CancellationException"));
    }

    public void testMultipleStrategiesAllGood() throws InterruptedException, SmokeTestException {
        String msg  = "message";
        int threadPoolSize  =   50;
        int timeoutSeconds  =   10;
        int strategyCount = threadPoolSize * 2;

        // Generate a list of test strategies
        Set<SmokeTestStrategy> smokeTestStrategies = new HashSet<>();

        // Build a list of strategy smoke tests that (each) will run in less that the
        // timeout for the overall test to complete in
        IntStream.range(1, strategyCount + 1)
                .forEach(i -> {
                            String id = "G" + i;
                            String responseMsg = msg + i;

                            smokeTestStrategies.add(new
                                    MockSimpleBaseSmokeTestStrategy(
                                    id,
                                    new SmokeTestResult("G" + i, SmokeTestResult.STATE.USER_PASS, 0, responseMsg)));
                        }
                );


        List<SmokeTestResult> smokeTestResults =
                SmokeTestContext.runSmokeTests(smokeTestStrategies, threadPoolSize, timeoutSeconds);

        assertNotNull(smokeTestResults);
        assertEquals(strategyCount, smokeTestResults.size());

        ///////////////////////////////////////
        // Make sure there are only USER_PASS's
        assertEquals(
                strategyCount,
                smokeTestResults
                    .stream()
                    .filter(s -> s.getState().equals(SmokeTestResult.STATE.USER_PASS))
                    .count());

    }

    public void testMultipleStrategiesSomeGoodSomeTimeout() throws InterruptedException, SmokeTestException {
        String msg  = "message1";
        int threadPoolSize      =   5;
        int timeoutSeconds      =   2;
        int goodStrategyCount   = threadPoolSize * 2;
        int badStrategyCount    = threadPoolSize;

        // Generate a list of test strategies
        Set<SmokeTestStrategy> smokeTestStrategies = new HashSet<>();

        // Build a list of strategy smoke tests that (each) will run in less that the
        // timeout for the overall test to complete in
        IntStream.range(1, goodStrategyCount + 1)
                .forEach(i -> {
                            String id = "G" + i;

                            smokeTestStrategies.add(new
                                    MockSimpleBaseSmokeTestStrategy(
                                    id,
                                    new SmokeTestResult(id, SmokeTestResult.STATE.USER_PASS, 0, msg + i)));
                        }
                );

        // Build a list of strategy smoke tests that (each) will run longer that the
        // timeout for the overall test to complete in
        IntStream.range(1, badStrategyCount + 1)
                .forEach(i -> {
                            String id = "B" + i;

                            smokeTestStrategies.add(new
                                    MockSimpleBaseSmokeTestStrategy(
                                    id,
                                    new SmokeTestResult(id, SmokeTestResult.STATE.USER_PASS, timeoutSeconds + 2, msg + i),
                                    false,
                                    timeoutSeconds + 2));
                        }
                );

        List<SmokeTestResult> smokeTestResults =
                SmokeTestContext.runSmokeTests(smokeTestStrategies, threadPoolSize, timeoutSeconds);

        assertNotNull(smokeTestResults);

        // Make sure there are some USER_PASS's
        assertEquals(
                goodStrategyCount,
                smokeTestResults
                        .stream()
                        .filter(s -> s.getState().equals(SmokeTestResult.STATE.USER_PASS))
                        .count());

        // Make sure there are some EXEC_ERROR's
        assertEquals(
                badStrategyCount,
                smokeTestResults
                        .stream()
                        .filter(s -> s.getState().equals(SmokeTestResult.STATE.EXEC_ERROR))
                        .count());
    }

    /**
     * A Mock implementation of the SmokeTestStrategy interface.
     *
     */
    class MockSimpleBaseSmokeTestStrategy extends BaseSmokeTestStrategy {
        private boolean throwException;
        private int delayInSeconds;

        /**
         * @param id A unique id for the test
         * @param smokeTestResult A mock result to pass back when validate is called
         */
        public MockSimpleBaseSmokeTestStrategy(
                final String id,
                final SmokeTestResult smokeTestResult) {
            this(id, smokeTestResult, false, 0);
        }

        /**
         *
         * @param id A unique id for the test
         * @param smokeTestResult A mock result to pass back when validate is called
         * @param throwException true to throw an Exception when execute is called; else false
         * @param delayInSeconds How long to emulate some work being done
         */
        public MockSimpleBaseSmokeTestStrategy(
                final String id,
                final SmokeTestResult smokeTestResult,
                final boolean throwException,
                final int delayInSeconds) {
            this.id                 = id;
            this.throwException     = throwException;
            this.smokeTestResult    = smokeTestResult;
            this.delayInSeconds     = delayInSeconds;
        }

        /**
         * Simulate doing some work
         *
         * @throws SmokeTestException
         */
        public void execute() throws SmokeTestException {
            if(throwException) {
                throw new RuntimeException(smokeTestResult.getMessage());
            }

            ///////////////////////////
            // Simulate processing time
            if(delayInSeconds > 0) {
                try {
                    sleep(1000 * delayInSeconds);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Retrieve the smoke test result
         *
         * @return A smoke test result
         */
        public SmokeTestResult validate() {

            return smokeTestResult;
        }
    }
}
