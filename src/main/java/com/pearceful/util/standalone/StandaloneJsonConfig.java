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

    /**
     *
     * @param configFile
     * @param cmdLineArgs
     * @param tag
     */
    public StandaloneJsonConfig(final String configFile, final String[] cmdLineArgs, final String tag) throws IOException, ScriptException {
        configure(configFile, cmdLineArgs, tag);
    }

    protected void configure(
            final String configFile,
            final String[] cmdLineArgs,
            final String tag) throws IOException, ScriptException {

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
        private String configFile;
        private String tag;
        private String[] cmdLineArgs;

        public JsonSetup(
                final String configFile,
                final String tag,
                final String[] cmdLineArgs,
                final Map<String, Object>config) {

            this.configFile = configFile;
            this.tag        = tag;
            this.cmdLineArgs= cmdLineArgs;

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
                            StringBuilder sb = new StringBuilder();
                            for(String arg : cmdLineArgs) {
                                sb.append(arg).append( " ");
                            }
                            systemVariables.put(varName, sb.toString().trim());
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

                Map<String, String> valueMap = (Map<String, String>)systemVar.get("inline_value_from_matching_tag");
                if(null != valueMap) {
                    ////////////////////////////////
                    // Find a match case insensitive
                    for(String key : valueMap.keySet()) {
                        if(key.equalsIgnoreCase(tag)) {
                            varVal = valueMap.get(key);
                            systemVariables.put(varName, varVal);
                        }
                    }
                    continue;
                }
            }
        }

        public String getConfigFile() {
            return configFile;
        }

        public String getTag() {
            return tag;
        }

        public String[] getCmdLineArgs() {
            return cmdLineArgs;
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
    }

    public static class JsonTestDefinition {
        private String id;
        private String cmd;
        private RUN run;
        private List<String> runTags = new ArrayList<>();

        public static enum RUN {
            ALWAYS,IF_TAG_MATCHES,UNLESS_TAG_MATCHES, NEVER
        };

        public JsonTestDefinition(final String id, final Map<String, Object>config) {
            this.id                     = id;

            Map<String, Object> entry   = (Map<String, Object>)config;

            cmd                          = (String)entry.get("cmd");
            Map<String, Object> action   = (Map<String, Object>)entry.get("run");

            Object always               =   action.get("always");
            Object ifTagMatches         =   action.get("if_tag_matches");
            Object unlessTagMMatches    =   action.get("unless_tag_matches");
            Object never                =   action.get("never");

            if(null != always) {
                run = RUN.ALWAYS;

            } else if(null != ifTagMatches) {
                run = RUN.IF_TAG_MATCHES;
                runTags.addAll((List<String>)ifTagMatches);

            } else if(null != unlessTagMMatches) {
                run = RUN.UNLESS_TAG_MATCHES;
                runTags.addAll((List<String>)unlessTagMMatches);

            } else if(null != never) {
                run = RUN.NEVER;

            }
        }

        public String getId() {
            return id;
        }

        public String getCmd() {
            return cmd;
        }

        public RUN getRun() {
            return run;
        }

        public List<String> getRunTags() {
            return runTags;
        }
    }
}
