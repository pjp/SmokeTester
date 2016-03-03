package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;
import junit.framework.TestCase;

import javax.script.ScriptException;
import java.io.IOException;

/**
 * Created by ppearce on 2016-02-28.
 */
public class StandaloneJsonConfigParseTest extends TestCase {
    private String jsonFile = "standalone/sample-conf.json";

    public void testReadConfig() throws IOException, ScriptException {
        String[] cmdLineArgs    = {"a", "b", "c"};
        String tag              = "DEV";

        StandaloneJsonConfig config =
                new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);

        assertNotNull(config);
    }

    public void testReadConfigSetup() throws IOException, ScriptException {
        String[] cmdLineArgs    = {"a", "b", "c"};
        String tag              = "DEV";

        StandaloneJsonConfig config =   new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);

        assertEquals(600, config.setup.getTimeoutSecondsForAllTests());
        assertEquals(5,   config.setup.getThreadPoolSize());
        assertEquals(9,   config.setup.getEnvronmentalVariables().size());

        assertEquals(jsonFile,                  config.setup.getEnvronmentalVariables().get("ST_CONFIG_FILE"));
        assertEquals(tag,                       config.setup.getEnvronmentalVariables().get("ST_TAG"));
        assertEquals(SmokeTestContext.VERSION,  config.setup.getEnvronmentalVariables().get("ST_VERSION"));
        assertEquals("600",                     config.setup.getEnvronmentalVariables().get("ST_TIMEOUT"));
        assertEquals("5",                       config.setup.getEnvronmentalVariables().get("ST_THREAD_POOL_SIZE"));

        assertEquals("Hello from DEV",  config.setup.getEnvronmentalVariables().get("ST_VALUE1"));
        assertEquals("Howzit",          config.setup.getEnvronmentalVariables().get("ST_VALUE2"));

        tag     =   "UAT";
        config  =    new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);
        assertEquals(tag,                       config.setup.getEnvronmentalVariables().get("ST_TAG"));
        assertEquals("Hello from UAT",          config.setup.getEnvronmentalVariables().get("ST_VALUE1"));
    }

    public void testReadConfigTest() throws IOException, ScriptException {
        String[] cmdLineArgs    = {"a", "b", "c"};
        String tag              = "DEV";

        StandaloneJsonConfig config =
                new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);

        assertEquals(5, config.testDefinitions.size());

        ////////////////////////////////////////////////////////////////////////////////
        StandaloneJsonConfig.JsonTestDefinition testDef = config.testDefinitions.get(0);
        assertNotNull(testDef);
        assertEquals("id1w", testDef.getId());
        assertEquals(
                "echo OS is [%ST_OS%] with selector tag [%ST_TAG%] and values [%ST_VALUE1%], [%ST_VALUE2%]",
                testDef.getCmd());

        assertEquals(StandaloneJsonConfig.JsonTestDefinition.RUN.ALWAYS, testDef.getRun());

        ////////////////////////////////////////////////////////////////////////////////
        testDef = config.testDefinitions.get(1);
        assertNotNull(testDef);
        assertEquals("id1u", testDef.getId());
        assertEquals(
                "echo OS is [$ST_OS] with selector tag [$ST_TAG] and values [$ST_VALUE1], [$ST_VALUE2]",
                testDef.getCmd());

        assertEquals(StandaloneJsonConfig.JsonTestDefinition.RUN.ALWAYS, testDef.getRun());
        assertEquals(0, testDef.getRunTags().size());

        ////////////////////////////////////////////////////////////////////////////////
        testDef = config.testDefinitions.get(2);
        assertNotNull(testDef);
        assertEquals("id2", testDef.getId());
        assertEquals(
                "hostname",
                testDef.getCmd());

        assertEquals(StandaloneJsonConfig.JsonTestDefinition.RUN.IF_TAG_MATCHES, testDef.getRun());
        assertEquals(2, testDef.getRunTags().size());
        assertEquals("DEV", testDef.getRunTags().get(0));
        assertEquals("SIT", testDef.getRunTags().get(1));

        ////////////////////////////////////////////////////////////////////////////////
        testDef = config.testDefinitions.get(3);
        assertNotNull(testDef);
        assertEquals("id3", testDef.getId());
        assertEquals(
                "xyz $ST_VALUE2",
                testDef.getCmd());

        assertEquals(StandaloneJsonConfig.JsonTestDefinition.RUN.UNLESS_TAG_MATCHES, testDef.getRun());

        ////////////////////////////////////////////////////////////////////////////////
        testDef = config.testDefinitions.get(4);
        assertNotNull(testDef);
        assertEquals("id4", testDef.getId());
        assertEquals(
                "",
                testDef.getCmd());

        assertEquals(StandaloneJsonConfig.JsonTestDefinition.RUN.NEVER, testDef.getRun());
    }
}
