/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.config;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoaderFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;


/**
 * The {@link AgentServiceConfigurationLoader} is a agent service configuration loader.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AgentServiceConfigurationLoader extends ConfigurationLoader implements IConfigurationLoaderFactory {
    private final Map<String, Object> initParameters;
    private File componentConfigurationFile;

    public AgentServiceConfigurationLoader() {
        this(null);
    }

    public AgentServiceConfigurationLoader(Map<String, Object> initParameters) {
        this.initParameters = initParameters;
    }

    @Override
    public ILoadContext loadConfiguration(String configurationLocation) {
        ConfigurationLoader loader = new ConfigurationLoader(initParameters);
        ILoadContext context = loader.loadConfiguration(configurationLocation);
        AgentConfiguration agentConfiguration = context.get(AgentConfiguration.SCHEMA);
        if (agentConfiguration == null)
            return super.loadConfiguration(configurationLocation);

        componentConfigurationFile = new File(System.getProperty("com.exametrika.workPath") + "/hotdeploy/conf/components/" +
                agentConfiguration.getComponent() + "/profiler.conf");
        if (!componentConfigurationFile.exists()) {
            componentConfigurationFile.getParentFile().mkdirs();
            FileWriter writer = null;
            try {
                JsonObject defaultProfiler;
                if (System.getProperty("com.exametrika.hostAgent") != null)
                    defaultProfiler = JsonUtils.EMPTY_OBJECT;
                else
                    defaultProfiler = Json.object()
                            .putObject("profiler")
                            .putObject("probes")
                            .putObject("stackProbe")
                            .put("instanceOf", "StackProbe")
                            .end()
                            .end()
                            .put("schemaVersion", 1)
                            .end().toObject();

                writer = new FileWriter(componentConfigurationFile);
                JsonSerializers.write(writer, defaultProfiler, true);
            } catch (Exception e) {
                Exceptions.wrapAndThrow(e);
            } finally {
                IOs.close(writer);
            }
        }

        return super.loadConfiguration(configurationLocation);
    }

    @Override
    public IConfigurationLoader createLoader(Map<String, Object> initParameters) {
        return new AgentServiceConfigurationLoader(initParameters);
    }

    @Override
    protected void processRootElement(JsonObjectBuilder rootElement, boolean schema) {
        if (schema || componentConfigurationFile == null)
            return;

        JsonArrayBuilder imports = rootElement.get("imports", null);
        if (imports == null) {
            imports = new JsonArrayBuilder();
            rootElement.put("imports", imports);
        }

        imports.add("file:" + componentConfigurationFile.getPath());
        componentConfigurationFile = null;
    }
}
