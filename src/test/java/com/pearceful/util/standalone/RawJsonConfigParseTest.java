package com.pearceful.util.standalone;

import junit.framework.TestCase;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Created by ppearce on 2016-02-28.
 */
public class RawJsonConfigParseTest extends TestCase{
    private ScriptEngine engine;
    private String jsonFile     = "standalone/sample-conf.json";

    @Override
    public void setUp() {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName("javascript");
    }

    public void testParseConfigTopLevels() throws IOException, ScriptException {
        String json = new String(
                Files.readAllBytes(
                        FileSystems.getDefault().getPath(jsonFile)));

        String script = "Java.asJSONCompatible(" + json + ")";

        ////////////////////////////////////////
        // Parse JSON and convert to Java Object
        Object result = this.engine.eval(script);
        assertTrue(result instanceof Map);

        //////////////////////////////////////////////////////////////////
        // JSON is now a Map of values, which could include Lists and Maps
        Map map = (Map)result;

        ///////////////////
        // Top level String
        assertTrue(map.containsKey("setup"));
        assertTrue(map.containsKey("test"));
    }

    public void testParseConfigSetup() throws IOException, ScriptException {
        String json = new String(
                Files.readAllBytes(
                        FileSystems.getDefault().getPath(jsonFile)));

        String script = "Java.asJSONCompatible(" + json + ")";

        ////////////////////////////////////////
        // Parse JSON and convert to Java Object
        Object result = this.engine.eval(script);
        assertTrue(result instanceof Map);

        //////////////////////////////////////////////////////////////////
        // JSON is now a Map of values, which could include Lists and Maps
        Map map = (Map)result;

        ///////////////////
        // Top level Map
        Map setup = (Map)map.get("setup");

        assertEquals(3, setup.size());
        assertTrue(setup.containsKey("timeout_seconds_for_all_tests"));
        assertTrue(setup.containsKey("thread_pool_size"));
        assertTrue(setup.containsKey("system_variables"));

        ///////////////////////
        // Check user variables
        List variables = (List)setup.get("system_variables");
        assertEquals(9, variables.size());
    }}
