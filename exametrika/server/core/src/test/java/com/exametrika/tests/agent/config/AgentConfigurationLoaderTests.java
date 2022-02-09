/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.agent.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.agent.config.AgentChannelConfiguration;
import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Debug;
import com.exametrika.impl.agent.config.AgentConfigurationLoader;
import com.exametrika.impl.profiler.config.ProfilerConfigurationLoader;

/**
 * The {@link AgentConfigurationLoaderTests} are tests for {@link AgentConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see ProfilerConfigurationLoader
 */
public class AgentConfigurationLoaderTests {
    @After
    public void tearDown() {
        Services.setDefaultRunModes(null);
    }

    @Test
    public void testAgentConfigurationLoad() {
        Services.setDefaultRunModes(Collections.singleton("agent"));
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("com.exametrika.home", tmpDir);
        System.setProperty("com.exametrika.nodeName", "testNode");
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        ILoadContext loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf");
        AgentConfiguration configuration = loadContext.get(AgentConfiguration.SCHEMA);
        TransportConfiguration transport = new TransportConfiguration(true, 1, 200, 3,
                4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15);
        AgentConfiguration configuration2 = new AgentConfiguration(new AgentChannelConfiguration("server", 1234,
                "bindAddress", true, "keyStorePath", "keyStorePassword", 1000, transport), "testAgent", "testComponent");
        assertThat(configuration, is(configuration2));

        ProfilerConfiguration profilerConfiguration = loadContext.get(ProfilerConfiguration.SCHEMA);
        assertThat(profilerConfiguration.getNodeName(), is("testAgent"));
        assertThat(profilerConfiguration.getNodeProperties(), is(Json.object().put("key1", "value1")
                .put("key2", "value2").toObject()));
        assertThat(profilerConfiguration.getWorkPath(), is(new File(tmpDir, "/work/profiler")));

        InstrumentationConfiguration instrumentationConfiguration = loadContext.get(InstrumentationConfiguration.SCHEMA);
        assertThat(instrumentationConfiguration.getDebugPath(), is(new File(tmpDir, "/work/instrument/debug")));

        loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config2.conf");
        AgentConfiguration configuration3 = loadContext.get(AgentConfiguration.SCHEMA);
        transport = new TransportConfiguration(Debug.isDebug(), 100, 1000, 5, 10000, 500, 500, 1000, 60000, 60000, 600000,
                7000000, 10000000, 100000000, 10000);
        AgentConfiguration configuration4 = new AgentConfiguration(new AgentChannelConfiguration("server", 17171,
                null, false, null, null, null, transport), "testNode", "testComponent");
        assertThat(configuration3, is(configuration4));

        profilerConfiguration = loadContext.get(ProfilerConfiguration.SCHEMA);
        assertThat(profilerConfiguration.getNodeName(), is("testNode"));
        assertThat(profilerConfiguration.getNodeProperties(), is(JsonUtils.EMPTY_OBJECT));
        assertThat(profilerConfiguration.getWorkPath(), is(new File(tmpDir, "/work/profiler")));

        instrumentationConfiguration = loadContext.get(InstrumentationConfiguration.SCHEMA);
        assertThat(instrumentationConfiguration.getDebugPath(), is(new File(tmpDir, "/work/instrument/debug")));
    }

    private static String getResourcePath() {
        String className = AgentConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
