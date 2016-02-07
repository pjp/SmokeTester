package com.pearceful.util;

import junit.framework.TestCase;

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

    public void testEnvValueSelectorForNull() {
        String line     = null ;
        int lineNumber  = 0;
        String env      = "DEV";
        String cmd      = "echo Howzit";

        assertEquals(null, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, null));
        assertEquals(null, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, env));

        /////////////////////////////////////////
        line = "#:DEV: " + cmd;
        assertEquals(null, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, env));

    }
    public void testEnvValueSelectorForValue() {
        String env      = "DEV";
        String value    = "echo Howzit";
        int lineNumber  = 0;
        String line     = String.format(
                "%s%s%s%s%s",
                ShellScriptListProcessor.COMMENT_LEADER,
                ShellScriptListProcessor.VALUE_SENTINAL,
                env,
                ShellScriptListProcessor.VALUE_SENTINAL,
                value) ;

        assertEquals(value, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, env));
        assertEquals(value, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, env.toLowerCase()));
        assertEquals(null, ShellScriptListProcessor.valueToBeSelected(lineNumber, line, "SIT"));
    }

    public void testEmptyOrNullFilter() {
        String filterText   =   null;

        try {
            ShellScriptListProcessor.LineFilter clf =
                    new ShellScriptListProcessor.LineFilter(filterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}

        ///////////////////////////////////////////////
        filterText  =   "";

        try {
            ShellScriptListProcessor.LineFilter clf =
                    new ShellScriptListProcessor.LineFilter(filterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}
    }

    public void testValidPlainFilter() {
        String rawFilterText    = " ";
        String cmdLine          =   "";
        int lineNumber          = 0;

        ShellScriptListProcessor.LineFilter clf =
                new ShellScriptListProcessor.LineFilter(rawFilterText);

        assertFalse(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One two";
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        rawFilterText    =
                String.format("%s ", ShellScriptListProcessor.LineFilter.PLAIN_FILTER_PREFIX);
        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s=", ShellScriptListProcessor.LineFilter.PLAIN_FILTER_PREFIX);
        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s0%s",
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL,
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL);
        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));
        assertFalse(clf.isMatch(1, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s0%s2%s3%s",
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL,
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL,
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL,
                        ShellScriptListProcessor.LineFilter.LINE_NUMBER_SENTINAL);
        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));
        assertFalse(clf.isMatch(1, cmdLine));
        assertTrue(clf.isMatch(2, cmdLine));
    }

    public void testValidPlainFilterInverted() {
        String rawFilterText    = " ";
        String cmdLine          = " ";
        int lineNumber          = 0;

        rawFilterText    =
                String.format("%s ", ShellScriptListProcessor.LineFilter.PLAIN_FILTER_PREFIX_INVERTED);
        ShellScriptListProcessor.LineFilter clf =
                new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertFalse(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s=", ShellScriptListProcessor.LineFilter.PLAIN_FILTER_PREFIX_INVERTED);
        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);
        assertFalse(clf.isMatch(lineNumber, cmdLine));

    }

    public void testValidRegexFilter() {
        String rawFilterText    =
                String.format("%s.*", ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX);
        String cmdLine  =   " ";
        int lineNumber          = 0;

        ShellScriptListProcessor.LineFilter clf =
                new ShellScriptListProcessor.LineFilter(rawFilterText);

        assertTrue(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s[0-9]+",
                        ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX);

        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);

        cmdLine =   "play 123";
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s [A-z]{3}",
                        ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX);

        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);

        cmdLine =   "play Six";
        assertTrue(clf.isMatch(lineNumber, cmdLine));
    }

    public void testValidRegexFilterInverted() {
        String rawFilterText    =
                String.format("%s.*", ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX_INVERTED);
        String cmdLine  =   "";
        int lineNumber          = 0;

        ShellScriptListProcessor.LineFilter clf =
                new ShellScriptListProcessor.LineFilter(rawFilterText);

        assertFalse(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s[0-9]+",
                        ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX_INVERTED);

        clf = new ShellScriptListProcessor.LineFilter(rawFilterText);

        cmdLine =   "play 123";
        assertFalse(clf.isMatch(lineNumber, cmdLine));

        cmdLine =   "play one two three";
        assertTrue(clf.isMatch(lineNumber, cmdLine));
    }

    public void testInvalidRegexFilter() {
        String rawFilterText    =
                String.format("%s*", ShellScriptListProcessor.LineFilter.REGEX_FILTER_PREFIX);

        try {
            ShellScriptListProcessor.LineFilter clf =
                    new ShellScriptListProcessor.LineFilter(rawFilterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}
    }
}
