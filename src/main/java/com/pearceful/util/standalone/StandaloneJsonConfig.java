package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by ppearce on 2016-02-29.
 */
public class StandaloneJsonConfig {

    public JsonSetup setup;
    public List<JsonTestDefinition> testDefinitions = new ArrayList<>();
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

        setup   = new JsonSetup(
                configFile,
                tag,
                cmdLineArgs,
                (Map<String, Object>)config.get("setup"));

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
        private Map<String, String> systemVariables = new HashMap<>();

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

        public JsonSetup(
                final String configFile,
                final String tag,
                final String[] cmdLineArgs,
                final Map<String, Object>config) {

            timeoutSecondsForAllTests                   = (int)config.get("timeout_seconds_for_all_tests");
            threadPoolSize                              = (int)config.get("thread_pool_size");
            List<Map<String, Object>> systemVars        = (List<Map<String, Object>>)config.get("system_variables");

            for(Map<String, Object>systemVar : systemVars) {
                String varName = (String)systemVar.get("name");

                String varVal  = (String)systemVar.get("internal_value");
                if(null != varVal) {
                    switch(varVal) {
                        case "VERSION":
                            systemVariables.put(varName, SmokeTestContext.VERSION);
                            break;
                        case "CONFIG_FILE":
                            systemVariables.put(varName, configFile);
                            break;
                        case "CMD_LINE":
                            systemVariables.put(varName, cmdLineArgs.toString());
                            break;
                        case "TIMEOUT":
                            systemVariables.put(varName, "" + timeoutSecondsForAllTests);
                            break;
                        case "THREAD_POOL_SIZE":
                            systemVariables.put(varName, "" + threadPoolSize);
                            break;
                        case "ID":
                            systemVariables.put(varName,"ID-PLACEHOLDER");
                            break;
                        case "OS":
                            String osName   = System.getProperty("os.name");
                            osName          = osName.toLowerCase(Locale.ENGLISH);

                            if (osName.indexOf("windows") != -1) {
                                systemVariables.put(varName, "windows");
                            } else {
                                systemVariables.put(varName, "unix");
                            }
                            break;
                        case "TAG":
                            systemVariables.put(varName, tag);
                            break;
                    }
                    continue;
                }

                varVal  = (String)systemVar.get("inline_value") ;
                if(null != varVal) {
                    systemVariables.put(varName, varVal);
                    continue;
                }

//                varVal  = (String)systemVar.get("inline_value_from_matching_tag");
//                if(null != varVal) {
//                    continue;
//                }
            }
        }

        public int getTimeoutSecondsForAllTests() {
            return timeoutSecondsForAllTests;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public Map<String, String> getSystemVariables() {
            return systemVariables;
        }

        public static enum VALUE_FROM {
            INTERNAL,
            INLINE_VALUE,
            INLINE_VALUE_FROM_MATCHING_TAG
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
