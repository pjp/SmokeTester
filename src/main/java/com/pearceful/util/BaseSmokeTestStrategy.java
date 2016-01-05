package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 */
public abstract class BaseSmokeTestStrategy< T extends BaseSmokeTestStrategy> implements SmokeTestStrategy, Comparable<T> {

    protected String id;
    protected SmokeTestResult smokeTestResult;

    public String getId() { return id; }

    public void preExecute() {
    }

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
