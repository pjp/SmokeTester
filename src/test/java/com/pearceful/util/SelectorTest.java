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
public class SelectorTest extends TestCase {
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

    public void testMustBeSelected() {
        String line = "" ;
        int lineNumber  = 0;
        String env      = "DEV";
        String cmd      = "echo Howzit";

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s %s",
                    ShellScriptListProcessor.COMMENT_LEADER,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    ShellScriptListProcessor.SELECTED_PREFIX,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "ANY_OLD"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s %s",
                    ShellScriptListProcessor.COMMENT_LEADER,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    env,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                    ShellScriptListProcessor.COMMENT_LEADER,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    "DEV",
                    ShellScriptListProcessor.TAG_SENTINAL,
                    "SIT",
                    ShellScriptListProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "SIT"));
    }

    public void testMustNotBeSelected() {
        String line     = "" ;
        int lineNumber  = 0;
        String env      = "dev";
        String cmd      = "echo Howzit";

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s-%s%s%s %s",
                    ShellScriptListProcessor.COMMENT_LEADER,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    ShellScriptListProcessor.NOT_SELECTED_PREFIX,
                    ShellScriptListProcessor.TAG_SENTINAL,
                    cmd);

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s %s",
                ShellScriptListProcessor.COMMENT_LEADER,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX,
                ShellScriptListProcessor.TAG_SENTINAL,
                env.toUpperCase(),
                ShellScriptListProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                ShellScriptListProcessor.COMMENT_LEADER,
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "DEV",
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "SIT",
                ShellScriptListProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "PROD"));
    }

    public void testSelectedAndNotSelected() {
        String line = "" ;
        int lineNumber  = 0;
        String cmd      = "echo Howzit";

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s%s%s %s",
                ShellScriptListProcessor.COMMENT_LEADER,
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "DEV",
                ShellScriptListProcessor.TAG_SENTINAL,
                "PROD",
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "SIT",
                ShellScriptListProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "PROD"));
    }

    public void testSelectedAndNotSelected2() {
        String line = "" ;
        int lineNumber  = 0;
        String cmd      = "echo Howzit";

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                ShellScriptListProcessor.COMMENT_LEADER,
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "DEV",
                ShellScriptListProcessor.TAG_SENTINAL,
                ShellScriptListProcessor.NOT_SELECTED_PREFIX + "SIT",
                ShellScriptListProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "PROD"));
        assertTrue(ShellScriptListProcessor.lineToBeSelected(lineNumber, line, "MickeyMouse"));
    }
}
