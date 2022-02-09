/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.metrics.host;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.metrics.host.config.ExpressionProcessNamingStrategyConfiguration;
import com.exametrika.api.metrics.host.config.HostCpuMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostFileSystemMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostMemoryMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostNetworkMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostProcessMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostKpiMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostSwapMonitorConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;

/**
 * The {@link HostMetricsConfigurationLoaderTests} are tests for configuration loader for JVM metrics.
 *
 * @author Medvedev-A
 */
@Ignore
public class HostMetricsConfigurationLoaderTests {
    @Test
    public void testMetricsConfigurationLoad() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File workDir = new File(tmpDir, "work/profiler");
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        ProfilerConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(ProfilerConfiguration.SCHEMA);

        ProfilerConfiguration configuration2 = new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(),
                Collections.asSet(
                        new HostCpuMonitorConfiguration("monitor1", "Scope1", 1000, null),
                        new HostFileSystemMonitorConfiguration("monitor2", "Scope1", 1000, null, "*"),
                        new HostMemoryMonitorConfiguration("monitor3", "Scope1", 1000, null),
                        new HostNetworkMonitorConfiguration("monitor4", "Scope1", 1000, null, "eth0", true, false, java.util.Collections.<String, Long>singletonMap("eth0", 100000000l)),
                        new HostProcessMonitorConfiguration("monitor5", "Scope1", 1000, null,
                                Arrays.asList("filter('*java*',name)", "filter('*python*',name)"),
                                new ExpressionProcessNamingStrategyConfiguration("name")),
                        new HostSwapMonitorConfiguration("monitor6", "Scope1", 1000, null),
                        new HostKpiMonitorConfiguration("monitor7", "Scope1", 1000, null, "host.kpi", "*", "eth0")
                ), Collections.<ProbeConfiguration>asSet(), 1, Runtime.getRuntime().availableProcessors() * 2, 100, 600000, 300000,
                workDir, 1000000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);

        assertThat(configuration2, is(configuration));
    }

    private static String getResourcePath() {
        String className = HostMetricsConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
