package com.pearceful.util.standalone;

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

        StandaloneJsonConfig config =
                new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);

        assertEquals(600, config.setup.getTimeoutSecondsForAllTests());
        assertEquals(2,   config.setup.getThreadPoolSize());
        assertEquals(10,  config.setup.getSystemVariables().size());

    }

    public void testReadConfigTest() throws IOException, ScriptException {
        String[] cmdLineArgs    = {"a", "b", "c"};
        String tag              = "DEV";

        StandaloneJsonConfig config =
                new StandaloneJsonConfig(jsonFile, cmdLineArgs, tag);

        assertEquals(4, config.testDefinitions.size());

    }
}
