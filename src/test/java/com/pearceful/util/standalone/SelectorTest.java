package com.pearceful.util.standalone;

import junit.framework.TestCase;

import static java.lang.Thread.sleep;

/**
 * Created by pjp on 2015-12-26.
 */
public class SelectorTest extends TestCase {
    public void testNullLeadingToken() {
        String line =   null ;

        assertEquals("", ConfigProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingToken() {
        String line =   "" ;

        assertEquals("", ConfigProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingTokenEmptyLine() {
        String line =   " " ;

        assertEquals("", ConfigProcessor.stripLeadingToken(line));
    }

    public void testNoLeadingTokenEmptyLineWithTab() {
        String line =   " \t " ;

        assertEquals("\t ", ConfigProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenEmptyLine() {
        String line =   " a" ;

        assertEquals("a", ConfigProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenEmptyLineWithTab() {
        String line =   " a\t" ;

        assertEquals("a\t", ConfigProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithSmallRestOfLine() {
        String line =   "a b" ;

        assertEquals("b", ConfigProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithLongRestOfLine() {
        String line =   "a b c " ;

        assertEquals("b c ", ConfigProcessor.stripLeadingToken(line));
    }

    public void testLeadingTokenWithLongRestOfLineNoTrailingSpace() {
        String line =   "a b c d" ;

        assertEquals("b c d", ConfigProcessor.stripLeadingToken(line));
    }

    public void testNullSelector() {
        String line = null ;
        int lineNumber  = 0;
        String env      = "";

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));
    }

    public void testEmptySelector() {
        String line = "" ;
        int lineNumber  = 0;
        String env      = "";

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        line = " \t" ;
        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));
    }

    public void testMustBeSelected() {
        String line = "" ;
        int lineNumber  = 0;
        String env      = "DEV";
        String cmd      = "echo Howzit";

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s %s",
                    ConfigProcessor.COMMENT_LEADER,
                    ConfigProcessor.TAG_SENTINAL,
                    ConfigProcessor.SELECTED_PREFIX,
                    ConfigProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "ANY_OLD"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s %s",
                    ConfigProcessor.COMMENT_LEADER,
                    ConfigProcessor.TAG_SENTINAL,
                    env,
                    ConfigProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                    ConfigProcessor.COMMENT_LEADER,
                    ConfigProcessor.TAG_SENTINAL,
                    "DEV",
                    ConfigProcessor.TAG_SENTINAL,
                    "SIT",
                    ConfigProcessor.TAG_SENTINAL,
                    cmd);

        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "SIT"));
    }

    public void testMustNotBeSelected() {
        String line     = "" ;
        int lineNumber  = 0;
        String env      = "dev";
        String cmd      = "echo Howzit";

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s-%s%s%s %s",
                    ConfigProcessor.COMMENT_LEADER,
                    ConfigProcessor.TAG_SENTINAL,
                    ConfigProcessor.NOT_SELECTED_PREFIX,
                    ConfigProcessor.TAG_SENTINAL,
                    cmd);

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s %s",
                ConfigProcessor.COMMENT_LEADER,
                ConfigProcessor.NOT_SELECTED_PREFIX,
                ConfigProcessor.TAG_SENTINAL,
                env.toUpperCase(),
                ConfigProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, env));

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                ConfigProcessor.COMMENT_LEADER,
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "DEV",
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "SIT",
                ConfigProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "PROD"));
    }

    public void testSelectedAndNotSelected() {
        String line = "" ;
        int lineNumber  = 0;
        String cmd      = "echo Howzit";

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s%s%s %s",
                ConfigProcessor.COMMENT_LEADER,
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "DEV",
                ConfigProcessor.TAG_SENTINAL,
                "PROD",
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "SIT",
                ConfigProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "PROD"));
    }

    public void testSelectedAndNotSelected2() {
        String line = "" ;
        int lineNumber  = 0;
        String cmd      = "echo Howzit";

        ///////////////////////////////////////////////////
        line    = String.format("%s%s%s%s%s%s %s",
                ConfigProcessor.COMMENT_LEADER,
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "DEV",
                ConfigProcessor.TAG_SENTINAL,
                ConfigProcessor.NOT_SELECTED_PREFIX + "SIT",
                ConfigProcessor.TAG_SENTINAL,
                cmd);

        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "DEV"));
        assertFalse(ConfigProcessor.lineToBeSelected(lineNumber, line, "SIT"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "UAT"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "PROD"));
        assertTrue(ConfigProcessor.lineToBeSelected(lineNumber, line, "MickeyMouse"));
    }

    public void testEnvValueSelectorForNull() {
        String line     = null ;
        String env      = "DEV";
        String cmd      = "echo Howzit";

        assertEquals(null, ConfigProcessor.valueToBeSelected(line, null));
        assertEquals(null, ConfigProcessor.valueToBeSelected(line, env));

        /////////////////////////////////////////
        line = "#:DEV: " + cmd;
        assertEquals(null, ConfigProcessor.valueToBeSelected(line, env));

    }
    public void testEnvValueSelectorForValue() {
        String env      = "DEV";
        String value    = "echo Howzit";
        String line     = String.format(
                "%s%s%s%s%s",
                ConfigProcessor.COMMENT_LEADER,
                ConfigProcessor.VALUE_SENTINAL,
                env,
                ConfigProcessor.VALUE_SENTINAL,
                value) ;

        assertEquals(value, ConfigProcessor.valueToBeSelected(line, env));
        assertEquals(value, ConfigProcessor.valueToBeSelected(line, env.toLowerCase()));
        assertEquals(null,  ConfigProcessor.valueToBeSelected(line, "SIT"));
    }

    public void testEmptyOrNullFilter() {
        String filterText   =   null;

        try {
            ConfigProcessor.TestSelectionFilter clf =
                    new ConfigProcessor.TestSelectionFilter(filterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}

        ///////////////////////////////////////////////
        filterText  =   "";

        try {
            ConfigProcessor.TestSelectionFilter clf =
                    new ConfigProcessor.TestSelectionFilter(filterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}
    }

    public void testValidPlainFilter() {
        String rawFilterText    = " ";
        String cmdLine          =   "";
        int lineNumber          = 0;

        ConfigProcessor.TestSelectionFilter clf =
                new ConfigProcessor.TestSelectionFilter(rawFilterText);

        assertFalse(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One two";
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        rawFilterText    =
                String.format("%s ", ConfigProcessor.TestSelectionFilter.PLAIN_FILTER_PREFIX);
        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s=", ConfigProcessor.TestSelectionFilter.PLAIN_FILTER_PREFIX);
        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s0%s",
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL,
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL);
        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));
        assertFalse(clf.isMatch(1, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s0%s2%s3%s",
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL,
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL,
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL,
                        ConfigProcessor.TestSelectionFilter.TEST_ID_SENTINAL);
        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertTrue(clf.isMatch(lineNumber, cmdLine));
        assertFalse(clf.isMatch(1, cmdLine));
        assertTrue(clf.isMatch(2, cmdLine));
    }

    public void testValidPlainFilterInverted() {
        String rawFilterText    = " ";
        String cmdLine          = " ";
        int lineNumber          = 0;

        rawFilterText    =
                String.format("%s ", ConfigProcessor.TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED);
        ConfigProcessor.TestSelectionFilter clf =
                new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertFalse(clf.isMatch(lineNumber, cmdLine));

        ////////////////////////////////////////////
        cmdLine = "One=two";
        rawFilterText    =
                String.format("%s=", ConfigProcessor.TestSelectionFilter.PLAIN_FILTER_PREFIX_INVERTED);
        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);
        assertFalse(clf.isMatch(lineNumber, cmdLine));

    }

    public void testValidRegexFilter() {
        String rawFilterText    =
                String.format("%s.*", ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX);
        String cmdLine  =   " ";
        int lineNumber          = 0;

        ConfigProcessor.TestSelectionFilter clf =
                new ConfigProcessor.TestSelectionFilter(rawFilterText);

        assertTrue(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s[0-9]+",
                        ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX);

        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);

        cmdLine =   "play 123";
        assertTrue(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s [A-z]{3}",
                        ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX);

        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);

        cmdLine =   "play Six";
        assertTrue(clf.isMatch(lineNumber, cmdLine));
    }

    public void testValidRegexFilterInverted() {
        String rawFilterText    =
                String.format("%s.*", ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED);
        String cmdLine  =   "";
        int lineNumber          = 0;

        ConfigProcessor.TestSelectionFilter clf =
                new ConfigProcessor.TestSelectionFilter(rawFilterText);

        assertFalse(clf.isMatch(lineNumber, cmdLine));

        /////////////////////////////////////
        rawFilterText   =
                String.format(
                        "%s[0-9]+",
                        ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX_INVERTED);

        clf = new ConfigProcessor.TestSelectionFilter(rawFilterText);

        cmdLine =   "play 123";
        assertFalse(clf.isMatch(lineNumber, cmdLine));

        cmdLine =   "play one two three";
        assertTrue(clf.isMatch(lineNumber, cmdLine));
    }

    public void testInvalidRegexFilter() {
        String rawFilterText    =
                String.format("%s*", ConfigProcessor.TestSelectionFilter.REGEX_FILTER_PREFIX);

        try {
            ConfigProcessor.TestSelectionFilter clf =
                    new ConfigProcessor.TestSelectionFilter(rawFilterText);

            fail("Should have thrown an exception");
        } catch(Exception e) {}
    }
}
