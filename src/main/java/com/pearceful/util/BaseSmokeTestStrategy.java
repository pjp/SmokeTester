package com.pearceful.util;

/**
 * Created by pjp on 2015-12-26.
 *
 * A helper class to make it simpler to implement the SmokeTestStrategy interface.
 */
public abstract class BaseSmokeTestStrategy implements SmokeTestStrategy {
    protected boolean called = false;
    protected String id;
    protected SmokeTestResult smokeTestResult;

    @Override
    public boolean wasCalled() {
        return called;
    }

    @Override
    public void setCalled() {
        called = true;
    }

    @Override
    public String getId() { return id; }

    @Override
    public void preExecute() {
    }

    @Override
    public void postExecute() {
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;

        if(null == obj) return false;

        if(! (this instanceof SmokeTestStrategy)) return false;

        SmokeTestStrategy other = (SmokeTestStrategy)obj;

        return id.equals(other.getId());
    }

    @Override
    public String toString() {
        return String.format("id [%s]", getId());
    }
}
