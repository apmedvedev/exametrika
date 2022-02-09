/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.metrics.jvm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.common.meters.config.CountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxAttributeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmBufferPoolMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmCodeMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmKpiMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunMemoryMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunThreadMonitorConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.profiler.monitors.MonitorContext;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.MonitorsTests.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestTimeService;

/**
 * The {@link JvmMonitorsTests} are tests for JVM metrics monitors.
 *
 * @author Medvedev-A
 */
public class JvmMonitorsTests {
    private MonitorContext context;

    @Before
    public void setUp() {
        TestTimeService timeService = new TestTimeService();
        context = new MonitorContext(new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000,
                Json.object().put("key", "value").toObject(), null), timeService,
                new TestMeasurementHandler(), new TaskQueue<Runnable>(), new HashMap(), null);
    }

    @Test
    public void testJvmBufferPoolMonitor() {
        MonitorConfiguration configuration = new JvmBufferPoolMonitorConfiguration("monitor1", "Scope1", 0, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));

        checkComponents(configuration, measurements);
    }

    @Test
    public void testJvmCodeMonitor() {
        MonitorConfiguration configuration = new JvmCodeMonitorConfiguration("monitor1", "Scope1", 0, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));

        checkComponents(configuration, measurements);
    }

    @Test
    public void testJvmKpiMonitor() throws Throwable {
        MonitorConfiguration configuration = new JvmKpiMonitorConfiguration("monitor1", "Scope1", "jvm.kpi", 0, null, 5000);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        measurements.clear();
        invokeGc();
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testJvmSunMemoryMonitor() throws Throwable {
        MonitorConfiguration configuration = new JvmSunMemoryMonitorConfiguration("monitor1", "Scope1", 0, null,
                new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                        new UniformHistogramFieldConfiguration(0, 2000, 20),
                        new InstanceFieldConfiguration(10, true)), false, 0),
                new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                        new UniformHistogramFieldConfiguration(0, 10000000, 20),
                        new InstanceFieldConfiguration(10, true)), false, 0),
                new LogConfiguration(true, new ExpressionLogFilterConfiguration(
                        "(parameters.end - parameters.start) > 0 && parameters.bytes > 10000"),
                        Arrays.asList(new LogMeterConfiguration("log.counter", new CounterConfiguration(true, true, 0), null,
                                new CountLogProviderConfiguration())), null, null, 100, 512,
                        10000, 50, 1000), null, 5000);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        measurements.clear();

        for (int i = 0; i < 10; i++) {
            invokeGc();
            monitor.measure(measurements, 0, 0, false);
        }

        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public synchronized void testJvmSunThreadMonitor() throws Throwable {
        MonitorConfiguration configuration = new JvmSunThreadMonitorConfiguration("monitor1", "Scope1", 0, null, true, true, true, 10, true);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        measurements.clear();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
            }
        }, "test");
        thread.start();
        Thread.sleep(100);

        monitor.measure(measurements, 0, 0, true);

        thread.join();

        monitor.measure(measurements, 0, 0, true);

        monitor.stop();

        assertThat(measurements.size() >= 1, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testJmxMonitor() throws Throwable {
        MonitorConfiguration configuration = new JmxMonitorConfiguration("monitor1", "Scope1", 0, null,
                "jvm.memory", "java.lang:type=Memory", Arrays.asList(
                new JmxAttributeConfiguration("used", new GaugeConfiguration(true), "HeapMemoryUsage",
                        "get($.exa.truncate('used0', 4, false))"),
                new JmxAttributeConfiguration("committed", new GaugeConfiguration(true), "HeapMemoryUsage",
                        "get('committed')")));
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        assertThat(measurements.size(), is(1));
        checkComponents(configuration, measurements);
    }

    private void invokeGc() throws InterruptedException {
        for (int i = 0; i < 1; i++) {
            new MeasurementStrategyManager();
            System.gc();
            Thread.sleep(100);
        }
    }

    private void checkComponents(MonitorConfiguration configuration, List<Measurement> measurements) {
        Set<ComponentValueSchemaConfiguration> components = new LinkedHashSet<ComponentValueSchemaConfiguration>();
        configuration.buildComponentSchemas(components);

        for (Measurement measurement : measurements) {
            ComponentValueSchemaConfiguration component = null;
            for (ComponentValueSchemaConfiguration c : components) {
                if (measurement.getId().getComponentType().equals(c.getName())) {
                    component = c;
                    break;
                }
            }
            assertThat(component != null, is(true));
            IComponentValueBuilder builder = component.createBuilder();
            builder.set(measurement.getValue());
        }
    }
}
