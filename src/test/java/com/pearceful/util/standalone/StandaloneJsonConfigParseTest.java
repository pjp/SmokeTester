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
}
