package com.pearceful.util.standalone;

import com.pearceful.util.SmokeTestContext;
import org.apache.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by ppearce on 2016-02-29.
 */
public class StandaloneJsonConfig {
    private static final Logger LOGGER                  = Logger.getLogger(StandaloneJsonConfig.class);

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
        Path jsonConfigPath     = FileSystems.getDefault().getPath(configFile);

        if(Files.notExists(jsonConfigPath)) {
            String errMsg =
                    String.format(
                            "JSON configuration file [%s] cannot be accessed, cannot continue",
                            configFile);
            LOGGER.error("configure: " + errMsg);
            throw new RuntimeException(errMsg);
        }

        String json = new String(Files.readAllBytes(jsonConfigPath));

        //////////////////
        // Semantic checks
        if(json.length() < 8) {
            String errMsg =
                    String.format(
                            "JSON configuration file [%s] contains too few characters, cannot continue",
                            configFile);
            LOGGER.error("configure: " + errMsg);
            throw new RuntimeException(errMsg);
        }

        //////////////////////////////////////////////////////////////
        // Could throw exceptions at this point if the JSON is invalid
        String script = "Java.asJSONCompatible(" + json + ")";
        Object result = engine.eval(script);

        //////////////////////////////
        // Get top level objects first
        Map config = (Map)result;

        Map<String, Object> jsonSetup           = (Map<String, Object>)config.get("setup");
        Map<String, Map<String, Object>> tests  = (Map<String, Map<String, Object>>)config.get("test");

        ////////////////
        // Sanity checks
        if(null == jsonSetup) {
            String errMsg =
                    String.format(
                            "No top level \"%s\" JSON object found in file %s, cannot continue",
                            "setup",
                            configFile);
            LOGGER.error("configure: " + errMsg);
            throw new RuntimeException(errMsg);
        }

        if(null == tests) {
            String errMsg =
                    String.format(
                            "No top level \"%s\" JSON object found in file %s, cannot continue",
                            "tests",
                            configFile);
            LOGGER.error("configure: " + errMsg);
            throw new RuntimeException(errMsg);
        }

        setup = new JsonSetup(configFile,
                                tag,
                                cmdLineArgs,
                                jsonSetup);

        for(Map.Entry<String, Map<String, Object>> entry : tests.entrySet()) {
            String id                           = entry.getKey();
            Map<String, Object>conf             = entry.getValue();

            try {
                JsonTestDefinition testDefinition = new JsonTestDefinition(id, conf);
                testDefinitions.add(testDefinition);
            } catch(RuntimeException e) {}
        }

        LOGGER.info("configure: Found "+testDefinitions.size() + " valid tests.");
    }

    public static class JsonSetup {
        private int timeoutSecondsForAllTests;
        private int threadPoolSize;
        private Map<String, String> systemVariables = new HashMap<>();
        private String configFile;
        private String tag;
        private String[] cmdLineArgs;

        @Override
        public String toString() {
            StringBuilder sysVars = new StringBuilder();
            for(Map.Entry<String, String> sysVar : systemVariables.entrySet()) {
                sysVars.append(sysVar.getKey()).append("=").append(sysVar.getValue()).append(" ");
            }

            StringBuilder cmdLine = new StringBuilder();
            for(String arg : cmdLineArgs) {
                cmdLine.append(arg).append(" ");
            }

            return String.format(
                    "cmdLineArgs [%s], configFile [%s], tag [%s], timeoutSecondsForAllTests [%d], threadPoolSize [%d], " +
                    "systemVariables [%s]",
                    cmdLine,
                    configFile,
                    tag,
                    timeoutSecondsForAllTests,
                    threadPoolSize,
                    sysVars.toString());
        }

        public JsonSetup(
                final String configFile,
                final String tag,
                final String[] cmdLineArgs,
                final Map<String, Object>config) {

            this.configFile = configFile;
            this.tag        = tag;
            this.cmdLineArgs= cmdLineArgs;

            timeoutSecondsForAllTests                   = (int)config.getOrDefault("timeout_seconds_for_all_tests", 0);
            threadPoolSize                              = (int)config.getOrDefault("thread_pool_size", 5);
            List<Map<String, Object>> systemVars        = (List<Map<String, Object>>)config.get("system_variables");

            if(null != systemVars && systemVars.size() > 0) {
                for (Map<String, Object> systemVar : systemVars) {
                    String varName = (String) systemVar.get("name");

                    String varVal = (String) systemVar.get("internal_value");
                    if (null != varVal) {
                        switch (varVal) {
                            case "VERSION":
                                systemVariables.put(varName, SmokeTestContext.VERSION);
                                break;
                            case "CONFIG_FILE":
                                systemVariables.put(varName, configFile);
                                break;
                            case "CMD_LINE":
                                StringBuilder sb = new StringBuilder();
                                for (String arg : cmdLineArgs) {
                                    sb.append(arg).append(" ");
                                }
                                systemVariables.put(varName, sb.toString().trim());
                                break;
                            case "TIMEOUT":
                                systemVariables.put(varName, "" + timeoutSecondsForAllTests);
                                break;
                            case "THREAD_POOL_SIZE":
                                systemVariables.put(varName, "" + threadPoolSize);
                                break;
                            case "OS":
                                if (JsonListProcessor.onWindows()) {
                                    systemVariables.put(varName, "windows");
                                } else {
                                    systemVariables.put(varName, "unix");
                                }
                                break;
                            case "TAG":
                                systemVariables.put(varName, tag);
                                break;
                            default:
                                LOGGER.warn("constructor: Unknown \"internal_value\" [" + varVal + "]");
                                break;
                        }
                        continue;
                    }

                    varVal = (String) systemVar.get("inline_value");
                    if (null != varVal) {
                        systemVariables.put(varName, varVal);
                        continue;
                    }

                    Map<String, String> valueMap = (Map<String, String>) systemVar.get("inline_value_from_matching_tag");
                    if (null != valueMap) {
                        ////////////////////////////////
                        // Find a match case insensitive
                        for (String key : valueMap.keySet()) {
                            if (key.equalsIgnoreCase(tag)) {
                                varVal = valueMap.get(key);
                                systemVariables.put(varName, varVal);
                            }
                        }
                        continue;
                    }
                }
            } else {
                LOGGER.info("constructor: No \"system_variables\" section defined.");
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

        @Override
        public String toString() {
            return String.format("id [%s], run [%s], cmd [%s]", id, run, cmd);
        }

        public JsonTestDefinition(final String id, final Map<String, Object>config) {
            this.id                     = id;

            Map<String, Object> entry   = (Map<String, Object>)config;

            cmd                          = (String)entry.get("cmd");
            Map<String, Object> action   = (Map<String, Object>)entry.get("run");

            ////////////////
            // Sanity checks
            if(null == cmd) {
                String errMsg =
                        String.format(
                                "No \"%s\" JSON object found for id [%s], cannot continue",
                                "cmd",
                                id);
                LOGGER.error("constructor: " + errMsg);
                throw new RuntimeException(errMsg);
            }

            if(null == action) {
                String errMsg =
                        String.format(
                                "No \"%s\" JSON object found for id [%s], cannot continue",
                                "run",
                                id);
                LOGGER.error("constructor: " + errMsg);
                throw new RuntimeException(errMsg);
            }

            Object always               =   action.get("always");
            Object ifTagMatches         =   action.get("if_tag_matches");
            Object unlessTagMMatches    =   action.get("unless_tag_matches");
            Object never                =   action.get("never");

            if(null != always) {
                if(cmd.trim().length() < 1) {
                    LOGGER.warn("constructor: Empty cmd for id " + id);
                }
                run = RUN.ALWAYS;

            } else if(null != ifTagMatches) {
                if(cmd.trim().length() < 1) {
                    LOGGER.warn("constructor: Empty cmd for id " + id);
                }

                run = RUN.IF_TAG_MATCHES;
                runTags.addAll((List<String>)ifTagMatches);

            } else if(null != unlessTagMMatches) {
                if(cmd.trim().length() < 1) {
                    LOGGER.warn("constructor: Empty cmd for id " + id);
                }

                run = RUN.UNLESS_TAG_MATCHES;
                runTags.addAll((List<String>)unlessTagMMatches);

            } else if(null != never) {
                run = RUN.NEVER;

            } else {
                LOGGER.info("constructor: Unknown \"run\" value");
            }

            LOGGER.info("constructor: " + this);
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
