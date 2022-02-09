/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.web.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.server.web.config.WebServerConfiguration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.services.Services;
import com.exametrika.impl.profiler.config.ProfilerConfigurationLoader;
import com.exametrika.impl.server.web.config.WebServerConfigurationLoader;

/**
 * The {@link WebServerConfigurationLoaderTests} are tests for {@link WebServerConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see ProfilerConfigurationLoader
 */
public class WebServerConfigurationLoaderTests {
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
        WebServerConfiguration configuration = loadContext.get(WebServerConfiguration.SCHEMA);
        WebServerConfiguration configuration2 = new WebServerConfiguration("testWebServer", 1234, 1235,
                "bindAddress", true, "keyStorePath", "keyStorePassword");
        assertThat(configuration, is(configuration2));

        loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config2.conf");
        WebServerConfiguration configuration3 = loadContext.get(WebServerConfiguration.SCHEMA);
        WebServerConfiguration configuration4 = new WebServerConfiguration("testHost", 8080, null,
                null, false, null, null);
        assertThat(configuration3, is(configuration4));
    }

    private static String getResourcePath() {
        String className = WebServerConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
