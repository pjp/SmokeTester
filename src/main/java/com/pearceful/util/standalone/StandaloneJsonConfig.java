package com.pearceful.util.standalone;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ppearce on 2016-02-29.
 */
public class StandaloneJsonConfig {

    private JsonSetup setup;
    private List<JsonTestDefinition> testDefinitions = new ArrayList<>();
    private String[] cmdLineArgs;
    private String tag;
    private String configFile;

    /**
     *
     * @param configFile
     * @param cmdLineArgs
     * @param tag
     */
    public StandaloneJsonConfig(final String configFile, final String[] cmdLineArgs, final String tag) throws IOException, ScriptException {
        this.configFile     = configFile;
        this.cmdLineArgs    = cmdLineArgs;
        this.tag            = tag;

        configure();
    }

    protected void configure() throws IOException, ScriptException {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine engine     = sem.getEngineByName("javascript");

        String json = new String(
                Files.readAllBytes(
                        FileSystems.getDefault().getPath(configFile)));

        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = engine.eval(script);

        Map config = (Map)result;

        setup   = new JsonSetup((Map<String, Object>)config.get("config"));

        Map<String, Map<String, Object>> tests = (Map<String, Map<String, Object>>)config.get("test");
        for(Map.Entry<String, Map<String, Object>> entry : tests.entrySet()) {
            String id                           = entry.getKey();
            Map<String, Object>conf             = entry.getValue();
            JsonTestDefinition testDefinition   = new JsonTestDefinition(id, conf);

            testDefinitions.add(testDefinition);
        }
    }

    public static class JsonSetup {
        private int timeoutSecondsForAllTests;
        private int threadPoolSize;
        private Map<String, String> systemVariables;

        public static enum VALUE_FROM {
            INTERNAL,
            INLINE_VALUE,
            INLINE_VALUE_FROM_MATCHING_TAG
        }
        public static enum INTERNAL_VALUE_NAMES {
            VERSION,
            CONFIG_FILE,
            CMD_LINE,
            TIMEOUT,
            THREAD_POOL_SIZE,
            ID,
            OS,
            TAG
        }

        public JsonSetup(final Map<String, Object>config) {
        }
    }

    public static class JsonTestDefinition {
        private String id;
        private String cmd;
        private RUN run;

        public static enum RUN {
            ALWAYS,IF_TAG_MATCHES,UNLESS_TAG_MATCHES, NEVER
        };

        public JsonTestDefinition(final String id, final Map<String, Object>config) {
            this.id = id;
        }
    }
}
