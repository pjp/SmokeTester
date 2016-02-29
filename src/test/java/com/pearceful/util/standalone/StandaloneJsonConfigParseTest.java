package com.pearceful.util.standalone;

import com.google.gson.Gson;
import junit.framework.TestCase;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

/**
 * Created by ppearce on 2016-02-28.
 */
public class StandaloneJsonConfigParseTest extends TestCase {
    private String jsonFile = "standalone/sample-conf.json";

    public void testReadConfig() throws IOException, ScriptException {
        String json = new String(
                Files.readAllBytes(
                        FileSystems.getDefault().getPath(jsonFile)));

        Gson gson = new Gson();

        StandaloneJsonConfig config = gson.fromJson(json, StandaloneJsonConfig.class);
        assertNotNull(config);
    }
}
