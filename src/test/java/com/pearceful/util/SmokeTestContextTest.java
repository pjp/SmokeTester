package com.pearceful.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

/**
 * Created by pjp on 2015-12-26.
 */
public class SmokeTestContextTest extends TestCase {
    public void testSimpleContext() throws InterruptedException, SmokeTestException {
        String id = "1";
        String msg  = "message1";
        SmokeTestResult.STATE state = SmokeTestResult.STATE.PASS;
        SmokeTestResult smokeTestResult = new SmokeTestResult(id, state, 0, msg);

        SimpleBaseSmokeTestStrategy simpleBaseSmokeTestStrategy = new SimpleBaseSmokeTestStrategy(id, smokeTestResult);

        SmokeTestContext smokeTestContext = new SmokeTestContext();
        List<SmokeTestStrategy> smokeTestStrategies = new ArrayList<>();
        smokeTestStrategies.add(simpleBaseSmokeTestStrategy);

        List<SmokeTestResult> smokeTestResults = smokeTestContext.runSmokeTests(smokeTestStrategies, 1, 2);

        assertNotNull(smokeTestResults);
        assertEquals(1, smokeTestResults.size());

        SmokeTestResult result = smokeTestResults.get(0);

        assertEquals(id, result.getId());
        assertEquals(state, result.getState());

    }

    public void testSimpleContextWithForcedException() throws SmokeTestException {
        int timeout = 1;
        String id = "1";
        String msg  = "forcedException";
        SmokeTestResult.STATE state = SmokeTestResult.STATE.ERROR;
        SmokeTestResult smokeTestResult = new SmokeTestResult(id, state, 0, msg);

        SimpleBaseSmokeTestStrategy simpleBaseSmokeTestStrategy =
                new SimpleBaseSmokeTestStrategy(id, smokeTestResult, true, timeout);

        SmokeTestContext smokeTestContext = new SmokeTestContext();
        List<SmokeTestStrategy> smokeTestStrategies = new ArrayList<>();
        smokeTestStrategies.add(simpleBaseSmokeTestStrategy);

        List<SmokeTestResult> results = smokeTestContext.runSmokeTests(smokeTestStrategies, 1, timeout);
        SmokeTestResult retrievedSmokeTestResult = results.get(0);
        assertEquals(SmokeTestResult.STATE.ERROR, retrievedSmokeTestResult.getState());
        assertEquals(msg, retrievedSmokeTestResult.getMessage());
    }

    public void testSimpleContextWithTimeout() throws SmokeTestException {
        int timeout = 1;
        String id = "1";
        String msg  = "forcedException";
        SmokeTestResult.STATE state = SmokeTestResult.STATE.ERROR;
        SmokeTestResult smokeTestResult = new SmokeTestResult(id, state, 0, msg);

        SimpleBaseSmokeTestStrategy simpleBaseSmokeTestStrategy =
                new SimpleBaseSmokeTestStrategy(id, smokeTestResult, false, timeout * 2);

        SmokeTestContext smokeTestContext = new SmokeTestContext();
        List<SmokeTestStrategy> smokeTestStrategies = new ArrayList<>();
        smokeTestStrategies.add(simpleBaseSmokeTestStrategy);

        List<SmokeTestResult> results = smokeTestContext.runSmokeTests(smokeTestStrategies, 1, timeout);
        SmokeTestResult retrievedSmokeTestResult = results.get(0);
        assertEquals(SmokeTestResult.STATE.ERROR, retrievedSmokeTestResult.getState());
        assertTrue(retrievedSmokeTestResult.getMessage().contains("CancellationException"));
    }

    public void testMultipleContexts() throws InterruptedException, SmokeTestException {
        String msg  = "message1";
        int threadPoolSize  =   3;
        int timeoutSeconds  =   10;
        int strategyCount = threadPoolSize * 2;

        // Generate a list of test strategies
        List<SmokeTestStrategy> smokeTestStrategies = new ArrayList<>();

        IntStream.range(1, strategyCount + 1)
                .forEach(i ->
                        smokeTestStrategies.add(new
                                SimpleBaseSmokeTestStrategy(
                                "" + i,
                                new SmokeTestResult("" + i, SmokeTestResult.STATE.PASS, 0, msg + i)))
                );


        SmokeTestContext smokeTestContext = new SmokeTestContext();

        List<SmokeTestResult> smokeTestResults =
                smokeTestContext.runSmokeTests(smokeTestStrategies, threadPoolSize, timeoutSeconds);

        assertNotNull(smokeTestResults);
        assertEquals(strategyCount, smokeTestResults.size());

        // Make sure there are only PASS's
        assertEquals(strategyCount,
                smokeTestResults
                    .stream()
                    .filter(s -> s.getState().equals(SmokeTestResult.STATE.PASS))
                    .count());
    }

    class SimpleBaseSmokeTestStrategy extends BaseSmokeTestStrategy {
        private boolean throwException;
        private int delayInSeconds;

        public SimpleBaseSmokeTestStrategy(
                final String id,
                final SmokeTestResult smokeTestResult) {
            this(id, smokeTestResult, false, 0);
        }

        public SimpleBaseSmokeTestStrategy(
                final String id,
                final SmokeTestResult smokeTestResult,
                final boolean throwException,
                final int delayInSeconds) {
            this.id                 = id;
            this.throwException     = throwException;
            this.smokeTestResult    = smokeTestResult;
            this.delayInSeconds     = delayInSeconds;
        }

        public void execute() throws SmokeTestException {
            if(throwException) {
                throw new RuntimeException(smokeTestResult.getMessage());
            }

            ///////////////////////////
            // Simulate processing time
            if(delayInSeconds > 0) {
                try {
                    sleep(1000 * delayInSeconds);
                } catch (InterruptedException e) {}
            }
        }

        public SmokeTestResult validate() {

            return smokeTestResult;
        }
    }
}
