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
        this.smokeTestStrategy = smokeTestStrategy;
    }

    public SmokeTestStrategy getSmokeTestStrategy() {
        return smokeTestStrategy;
    }

    public SmokeTestResult call() throws Exception {
        SmokeTestResult smokeTestResult ;
        long startNs                    = System.nanoTime();

        try {
            LOGGER.debug(String.format("call: Processing test [%s]", smokeTestStrategy));

            smokeTestStrategy.preExecute();
            smokeTestStrategy.execute();
            smokeTestStrategy.postExecute();

            smokeTestResult = smokeTestStrategy.validate();
        } catch (Throwable t) {
            LOGGER.warn(String.format("call: Problem running test [%s]", smokeTestStrategy), t);

            smokeTestResult =
                    new SmokeTestResult(
                            smokeTestStrategy.getId(),
                            SmokeTestResult.STATE.ERROR,
                            System.nanoTime() - startNs,
                            t.getMessage());
        }

        return smokeTestResult;
    }
}
