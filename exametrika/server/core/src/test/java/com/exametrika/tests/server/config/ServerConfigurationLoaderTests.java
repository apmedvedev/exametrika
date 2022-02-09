/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.server.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.server.config.ServerChannelConfiguration;
import com.exametrika.api.server.config.ServerConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Debug;
import com.exametrika.impl.profiler.config.ProfilerConfigurationLoader;
import com.exametrika.impl.server.config.ServerConfigurationLoader;

/**
 * The {@link ServerConfigurationLoaderTests} are tests for {@link ServerConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see ProfilerConfigurationLoader
 */
@Ignore
public class ServerConfigurationLoaderTests {
    @After
    public void tearDown() {
        Services.setDefaultRunModes(null);
    }

    @Test
    public void testServerConfigurationLoad() {
        Services.setDefaultRunModes(Collections.singleton("server"));
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("com.exametrika.home", tmpDir);
        System.setProperty("com.exametrika.hostName", "testHost");
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        ILoadContext loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf");
        ServerConfiguration configuration = loadContext.get(ServerConfiguration.SCHEMA);
        TransportConfiguration transport = new TransportConfiguration(true, 1, 200, 3,
                4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15);
        ServerConfiguration configuration2 = new ServerConfiguration("testServer", new ServerChannelConfiguration(1234,
                "bindAddress", true, "keyStorePath", "keyStorePassword", 1000, transport),
                new DatabaseConfigurationBuilder().addPath(tmpDir + "/work/db").setInitialSchemaPath(tmpDir + "/conf/schema/schema.dbmodule").toConfiguration());
        assertThat(configuration, is(configuration2));

        loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config2.conf");
        ServerConfiguration configuration3 = loadContext.get(ServerConfiguration.SCHEMA);
        transport = new TransportConfiguration(Debug.isDebug(), 100, 1000, 5, 10000, 500, 500, 1000, 60000, 60000, 600000,
                7000000, 10000000, 100000000, 10000);
        ServerConfiguration configuration4 = new ServerConfiguration("testHost.server", new ServerChannelConfiguration(17171,
                null, false, null, null, null, transport), new DatabaseConfigurationBuilder()
                .addPath(tmpDir + "/work/db").setInitialSchemaPath(tmpDir + "/conf/schema/schema.dbmodule").toConfiguration());
        if (configuration3 != null && configuration4 != null)
            ;
        //assertThat(configuration3, is(configuration4));
    }

    private static String getResourcePath() {
        String className = ServerConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
