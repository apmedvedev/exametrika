/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.metrics.jvm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.api.metrics.jvm.config.FileProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.GcFilterConfiguration;
import com.exametrika.api.metrics.jvm.config.HttpConnectionProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.HttpServletProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JdbcConnectionProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JdbcProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmsConsumerProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmsProducerProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxAttributeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JulProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmBufferPoolMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmCodeMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmKpiMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunMemoryMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunThreadMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.Log4jProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.LogbackProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.TcpProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.UdpProbeConfiguration;
import com.exametrika.api.profiler.config.CompositeRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.HotspotRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.SimpleRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.ThresholdRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration.PrimaryType;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link JvmMetricsConfigurationLoaderTests} are tests for configuration loader for JVM metrics.
 *
 * @author Medvedev-A
 */
@Ignore
public class JvmMetricsConfigurationLoaderTests {
    @Test
    public void testMetricsConfigurationLoad() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File workDir = new File(tmpDir, "work/profiler");
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        ProfilerConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(ProfilerConfiguration.SCHEMA);

        RequestMappingStrategyConfiguration mappingStrategy = new CompositeRequestMappingStrategyConfiguration(Arrays.asList(
                new ThresholdRequestMappingStrategyConfiguration("name", "metadata", "parameters", "filter", "begin", "end",
                        123, 124, 125, 126, 50),
                new HotspotRequestMappingStrategyConfiguration("name", "metadata", "parameters", "filter", "begin", "end",
                        124, 125, 126, 127, 128, 29, 130, null, false),
                new SimpleRequestMappingStrategyConfiguration("name", "metadata", "parameters", null)
        ));
        ProbeConfiguration probeConfiguration1 = new JulProbeConfiguration("probe1", "default", 1000, null, 60000,
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration2 = new Log4jProbeConfiguration("probe2", "default", 1000, null, 60000,
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration3 = new LogbackProbeConfiguration("probe3", "default", 1000, null, 60000,
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration4 = new HttpServletProbeConfiguration("probe4", "default", null, 60000,
                mappingStrategy,
                2, new CounterConfiguration(true, false, 0), new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100), "testExpression", null,
                PrimaryType.YES, true,
                new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration5 = new HttpConnectionProbeConfiguration("probe5", "default", null, 60000,
                new SimpleRequestMappingStrategyConfiguration("name", "metadata", "parameters", null),
                new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration6 = new JmsConsumerProbeConfiguration("probe6", "default", null, 60000,
                new SimpleRequestMappingStrategyConfiguration("name", "metadata", "parameters", null),
                2, new CounterConfiguration(true, false, 0), new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100), null, null,
                PrimaryType.NO, true, new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0), new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration7 = new JmsProducerProbeConfiguration("probe7", "default", null, 60000,
                new CounterConfiguration(true, false, 0));
        ProbeConfiguration probeConfiguration8 = new FileProbeConfiguration("probe8", "default", null, 60000,
                new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0));
        ProbeConfiguration probeConfiguration9 = new TcpProbeConfiguration("probe9", "default", null, 60000,
                new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0));
        ProbeConfiguration probeConfiguration10 = new UdpProbeConfiguration("probe10", "default", null, 60000,
                new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0));
        ProbeConfiguration probeConfiguration11 = new JdbcProbeConfiguration("probe11", "default", null, 60000,
                new SimpleRequestMappingStrategyConfiguration("name", "metadata", "parameters", null), new CounterConfiguration(true, false, 0));
        ProbeConfiguration probeConfiguration12 = new JdbcConnectionProbeConfiguration("probe12", "default", null, 60000,
                new SimpleRequestMappingStrategyConfiguration("name", "metadata", "parameters", null), new CounterConfiguration(true, false, 0));

        ProfilerConfiguration configuration2 = new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(),
                Collections.asSet(
                        new JvmBufferPoolMonitorConfiguration("monitor1", "Scope1", 1000, null),
                        new JvmCodeMonitorConfiguration("monitor2", "Scope1", 1000, null),
                        new JvmKpiMonitorConfiguration("monitor3", "Scope1", "jvm.kpi", 1000, null, 5000),
                        new JvmSunMemoryMonitorConfiguration("monitor4", "Scope1", 1000, null,
                                new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                                        new UniformHistogramFieldConfiguration(0, 2000, 20),
                                        new InstanceFieldConfiguration(10, true)), false, 0),
                                new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                                        new LogarithmicHistogramFieldConfiguration(0, 40),
                                        new InstanceFieldConfiguration(10, true)), false, 0),
                                new LogConfiguration(true, new ExpressionLogFilterConfiguration(
                                        "(parameters.end - parameters.start) > 100 || parameters.bytes > 100000"), null, null, null,
                                        100, 512, 1000, 10, 100),
                                null, 5000),
                        new JvmSunMemoryMonitorConfiguration("monitor5", "Scope1", 1000, null,
                                new CounterConfiguration(true, false, 0),
                                new CounterConfiguration(true, false, 0),
                                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100),
                                new GcFilterConfiguration(200, 1000), 5000),
                        new JvmSunThreadMonitorConfiguration("monitor6", "Scope1", 1000, null, true, true, true, 10, true),
                        new JmxMonitorConfiguration("monitor7", "Scope1", 1000, null, "componentType", "java.lang:type=Memory",
                                Arrays.asList(new JmxAttributeConfiguration("metricType1", new GaugeConfiguration(true), "HeapMemoryUsage", "used"),
                                        new JmxAttributeConfiguration("metricType2", new GaugeConfiguration(true), "HeapMemoryUsage", "committed")))
                ), Collections.asSet(probeConfiguration1, probeConfiguration2, probeConfiguration3, probeConfiguration4,
                probeConfiguration5, probeConfiguration6, probeConfiguration7, probeConfiguration8, probeConfiguration9,
                probeConfiguration10, probeConfiguration11, probeConfiguration12),
                1, Runtime.getRuntime().availableProcessors() * 2, 100, 600000, 300000, workDir, 1000000,
                Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);

        assertThat(sort(configuration2.getProbes()), is(sort(configuration.getProbes())));
        assertThat(configuration2, is(configuration));
    }

    private List<ProbeConfiguration> sort(Set<ProbeConfiguration> probes) {
        List<ProbeConfiguration> list = new ArrayList<ProbeConfiguration>(probes);
        java.util.Collections.sort(list, new Comparator<ProbeConfiguration>() {
            @Override
            public int compare(ProbeConfiguration o1, ProbeConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return list;
    }

    private static String getResourcePath() {
        String className = JvmMetricsConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
