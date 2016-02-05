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
public class TagSelectorTest extends TestCase {
    public void testNullLeadingToken() {
        String line =   null ;

        assertEquals("", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingToken() {
        String line =   "" ;

        assertEquals("", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingTokenEmptyLine() {
        String line =   " " ;

        assertEquals("", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingTokenEmptyLineWithTab() {
        String line =   " \t " ;

        assertEquals("\t ", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenEmptyLine() {
        String line =   " a" ;

        assertEquals("a", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenEmptyLineWithTab() {
        String line =   " a\t" ;

        assertEquals("a\t", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithSmallRestOfLine() {
        String line =   "a b" ;

        assertEquals("b", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithLongRestOfLine() {
        String line =   "a b c " ;

        assertEquals("b c ", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithLongRestOfLineNoTrailingSpace() {
        String line =   "a b c d" ;

        assertEquals("b c d", ShellScriptListProcessor.stripLeadingToken(line));
    }

    public void testNullSelector() {
        String line = null ;
        int lineNumber  = 0;
        String env      = "";

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));
    }

    public void testEmptySelector() {
        String line = "" ;
        int lineNumber  = 0;
        String env      = "";

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        line = " \t" ;
        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));
    }
}
