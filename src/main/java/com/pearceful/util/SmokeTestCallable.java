package com.pearceful.util;

import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

/**
 * Created by pjp on 2015-12-27.
 *
 * A Callable to execute the defined strategy.
 */
public class SmokeTestCallable implements Callable {
    private static final Logger LOGGER                  = Logger.getLogger(SmokeTestCallable.class);

    private SmokeTestStrategy smokeTestStrategy;

    public  SmokeTestCallable(final SmokeTestStrategy smokeTestStrategy) {
        if(null == smokeTestStrategy) {
            throw new RuntimeException("Null SmokeTestStrategy specified");
        }

        this.smokeTestStrategy = smokeTestStrategy;
    }

    public SmokeTestStrategy getSmokeTestStrategy() {
        return smokeTestStrategy;
    }

    /**
     * Execute the strategy.
     *
     * @return A SmokeTestResult regardless of any Exceptions
     */
    @Override
    public SmokeTestResult call() {

        long startNs                    = System.nanoTime();
        SmokeTestResult smokeTestResult ;

        try {
            LOGGER.debug(String.format("call: Processing test id [%s]", smokeTestStrategy.getId()));

            smokeTestStrategy.preExecute();
            smokeTestStrategy.execute();
            smokeTestStrategy.postExecute();

            smokeTestResult = smokeTestStrategy.validate();

            long elapsedUs = (System.nanoTime() - startNs) / 1000;

            LOGGER.debug(String.format("call: Test result [%s] retrieved in %d uS", smokeTestResult, elapsedUs));
        } catch (Throwable t) {
            long elapsedUs = (System.nanoTime() - startNs) / 1000;

            LOGGER.warn(String.format("call: Problem running test id [%s] after %d uS", smokeTestStrategy.getId(), elapsedUs), t);

            smokeTestResult =
                    new SmokeTestResult(
                            smokeTestStrategy.getId(),
                            SmokeTestResult.STATE.USER_ERROR,
                            System.nanoTime() - startNs,
                            t.getMessage());
        }

        return smokeTestResult;
    }
}
