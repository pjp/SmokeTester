package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * A helper class to make it simpler to implement the SmokeTestStrategy interface.
 */
public abstract class BaseSmokeTestStrategy< T extends BaseSmokeTestStrategy> implements SmokeTestStrategy, Comparable<T> {

    protected String id;
    protected SmokeTestResult smokeTestResult;

    @Override
    public String getId() { return id; }

    @Override
    public void preExecute() {
    }

    @Override
    public void postExecute() {
    }

    public int compareTo(T o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return String.format("id [%s]", getId());
    }
}
