/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.boot.IAgentMXBean;
import com.exametrika.api.boot.config.BootConfiguration;
import com.exametrika.impl.boot.Bootstrap;
import com.exametrika.impl.boot.utils.JsonReader;
import com.exametrika.impl.boot.utils.Utils;

/**
 * The {@link BootConfigurationLoader} represents a loader of boot configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BootConfigurationLoader {
    private Set<String> loadedConfigurations = new HashSet<String>();

    public BootConfiguration load(String configurationPath) {
        return load(true, configurationPath, null);
    }

    private BootConfiguration load(boolean root, String configurationPath, String contextPath) {
        int pos = configurationPath.indexOf(':');
        if (pos != -1) {
            String schema = configurationPath.substring(0, pos);
            if (!schema.equals("file") && !schema.equals("inline"))
                pos = -1;
        }

        if (pos == -1) {
            if (contextPath == null)
                configurationPath = "file:" + configurationPath;
            else
                configurationPath = "file:" + new File(contextPath, configurationPath).toString();

            pos = configurationPath.indexOf(':');
        }

        if (loadedConfigurations.contains(configurationPath))
            return null;

        loadedConfigurations.add(configurationPath);
        String schema = configurationPath.substring(0, pos);
        configurationPath = configurationPath.substring(pos + 1);
        contextPath = new File(configurationPath).getParent().toString();

        if (schema.equals("file"))
            return loadFromFile(root, configurationPath, contextPath);
        else if (schema.equals("inline"))
            return loadFromString(root, configurationPath, contextPath);
        else
            throw new IllegalArgumentException("Unknown schema of configuration path.");
    }

    private BootConfiguration loadFromFile(boolean root, String configurationPath, String contextPath) {
        InputStream stream = null;

        try {
            stream = new FileInputStream(configurationPath);
            String contents = load(stream, "UTF-8");
            return loadFromString(root, contents, contextPath);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private BootConfiguration loadFromString(boolean root, String contents, String contextPath) {
        contents = Utils.expandProperties(contents, true);
        Handler handler = new Handler(root, contextPath);
        JsonReader reader = new JsonReader(contents, handler);
        reader.read();

        return handler.createConfiguration();
    }

    public static String load(InputStream stream, String charset) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        try {
            boolean first = true;
            while (true) {
                String str = reader.readLine();
                if (str == null)
                    break;

                if (first)
                    first = false;
                else
                    builder.append(lineSeparator);

                builder.append(str);
            }

            return builder.toString();
        } finally {
            reader.close();
        }
    }

    private class Handler implements JsonReader.IJsonHandler {
        private final String contextPath;
        private String key;
        private LinkedList<String> path = new LinkedList<String>();
        private final Set<String> runModes = new HashSet<String>();
        private final List<String> systemPackages = new ArrayList<String>();
        private final List<File> bootClassPaths = new ArrayList<File>();
        private final List<File> systemClassPaths = new ArrayList<File>();
        private final List<File> classPaths = new ArrayList<File>();
        private final List<File> libraryPaths = new ArrayList<File>();
        private File workPath;
        private File hotDeployConfigPath;
        private File hotDeployModulesPath;
        private long hotDeployDetectionPeriod = 1000;
        private long hotDeployRedeploymentPeriod = 5000;
        private long hotDeployRestartDelayPeriod = 5000;
        private boolean productionMode;

        public Handler(boolean root, String contextPath) {
            this.contextPath = contextPath;
            String home = System.getProperty(IAgentMXBean.HOME_PROPERTY_NAME);

            if (root)
                systemPackages.addAll(Arrays.asList("java", "javax", "org.w3c", "org.xml", "sun", "sunw", "com.sun", "org.ietf.jgss",
                        "org.omg", "org.jcp.xml", "jdk", Bootstrap.class.getPackage().getName()));

            workPath = new File(home, "work");
        }

        @Override
        public void startText() {
        }

        @Override
        public void endText() {
        }

        @Override
        public void startObject() {
            path.addLast(key);
            key = null;
        }

        @Override
        public void endObject() {
            path.removeLast();
        }

        @Override
        public void startArray() {
            path.addLast(key);
            key = null;
        }

        @Override
        public void endArray() {
            path.removeLast();
        }

        @Override
        public void key(String key) {
            this.key = key;
        }

        @Override
        public void value(Object value) {
            if (path.equals(Arrays.asList(null, "imports")))
                processImport((String) value);
            else if (path.equals(Arrays.asList(null, "boot", "runModes")))
                runModes.add((String) value);
            else if (path.equals(Arrays.asList(null, "boot", "bootClassPath")))
                bootClassPaths.add(new File((String) value));
            else if (path.equals(Arrays.asList(null, "boot", "systemClassPath")))
                systemClassPaths.add(new File((String) value));
            else if (path.equals(Arrays.asList(null, "boot", "classPath")))
                classPaths.add(new File((String) value));
            else if (path.equals(Arrays.asList(null, "boot", "libraryPath")))
                libraryPaths.add(new File((String) value));
            else if (path.equals(Arrays.asList(null, "boot", "systemPackages")))
                systemPackages.add((String) value);
            else if (path.equals(Arrays.asList(null, "boot", "workPath")))
                workPath = new File((String) value);
            else if (path.equals(Arrays.asList(null, "boot", "hotDeploy"))) {
                if (key.equals("configPath"))
                    hotDeployConfigPath = new File((String) value);
                else if (key.equals("modulesPath"))
                    hotDeployModulesPath = new File((String) value);
                else if (key.equals("detectionPeriod"))
                    hotDeployDetectionPeriod = (Long) value;
                else if (key.equals("redeploymentPeriod"))
                    hotDeployRedeploymentPeriod = (Long) value;
                else if (key.equals("restartDelayPeriod"))
                    hotDeployRestartDelayPeriod = (Long) value;
            } else if (path.equals(Arrays.asList(null, "common"))) {
                if (key.equals("runtimeMode") && value.equals("production"))
                    productionMode = true;
            }
        }

        private void processImport(String configurationPath) {
            BootConfiguration configuration = load(false, configurationPath, contextPath);
            if (configuration == null)
                return;

            bootClassPaths.addAll(configuration.getBootClassPaths());
            systemClassPaths.addAll(configuration.getSystemClassPaths());
            classPaths.addAll(configuration.getClassPaths());
            libraryPaths.addAll(configuration.getLibraryPaths());
            systemPackages.addAll(configuration.getSystemPackages());
        }

        public BootConfiguration createConfiguration() {
            if (System.getProperty(IAgentMXBean.WORKPATH_PROPERTY_NAME) != null)
                workPath = new File(System.getProperty(IAgentMXBean.WORKPATH_PROPERTY_NAME));

            return new BootConfiguration(productionMode, runModes, bootClassPaths, systemClassPaths, classPaths,
                    libraryPaths, systemPackages, workPath, hotDeployConfigPath != null ? hotDeployConfigPath : new File(workPath, "hotdeploy/conf"),
                    hotDeployModulesPath != null ? hotDeployModulesPath : new File(workPath, "hotdeploy/lib"),
                    hotDeployDetectionPeriod, hotDeployRedeploymentPeriod, hotDeployRestartDelayPeriod);
        }
    }
}
