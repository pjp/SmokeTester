package com.pearceful.util.standalone;

import junit.framework.TestCase;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by ppearce on 2016-02-28.
 */
public class InternalJsonParseTest extends TestCase{
    private ScriptEngine engine;
    private String jsonFile     = "src/test/resources/standalone/misc.json";
    private String badJsonFile  = "src/test/resources/standalone/malformed-misc.json";

    @Override
    public void setUp() {
        ScriptEngineManager sem = new ScriptEngineManager();
        this.engine = sem.getEngineByName("javascript");
    }

    public void testParseBadNestedJson() throws IOException, ScriptException {
        String json = new String(
                Files.readAllBytes(
                        FileSystems.getDefault().getPath(badJsonFile)));

        String script = "Java.asJSONCompatible(" + json + ")";

        ////////////////////////////////////////
        // Parse JSON and convert to Java Object
        try {
            this.engine.eval(script);
            fail("Should have thrown an exception");
        } catch(ScriptException e) {}
    }

    public void testParseNestedJson() throws IOException, ScriptException {
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
        assertTrue(map.containsKey("status"));
        String status = (String)map.get("status");
        assertEquals("green", status);

        ////////////////////
        // Nested structures
        assertTrue(map.containsKey("nodes"));
        Map nodes = (Map)map.get("nodes");
        assertNotNull(nodes);

        ///////////////
        // Nested Lists
        List<String> versions = (List<String>)nodes.get("versions");
        assertNotNull(versions);

        assertEquals("1.7.2", versions.get(0));

        //////////////
        // Nested Maps
        Map count = (Map)nodes.get("count");
        assertNotNull(count);

        int masterData = (Integer)count.get("master_data");
        assertEquals(5, masterData);


    }

}
