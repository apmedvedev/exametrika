/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.metrics.host;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.metrics.host.config.ExpressionProcessNamingStrategyConfiguration;
import com.exametrika.api.metrics.host.config.HostCpuMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostFileSystemMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostKpiMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostMemoryMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostNetworkMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostProcessMonitorConfiguration;
import com.exametrika.api.metrics.host.config.HostSwapMonitorConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.profiler.monitors.MonitorContext;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.MonitorsTests.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestTimeService;

/**
 * The {@link HostMonitorsTests} are tests for Host metrics monitors.
 *
 * @author Medvedev-A
 */
public class HostMonitorsTests {
    private MonitorContext context;

    @Before
    public void setUp() {
        JsonObject nodeProperties = Json.object()
                .putArray("groups")
                .add("hosts.group1.group11")
                .add("hosts.group1.group12")
                .add("hosts.group2")
                .end()
                .putObject("groupsMetadata")
                .putObject("hosts.group1.group11")
                .put("hierarchyType", "default")
                .end()
                .end()
                .toObject();
        TestTimeService timeService = new TestTimeService();
        context = new MonitorContext(new ProfilerConfiguration("host", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, nodeProperties, null),
                timeService, new TestMeasurementHandler(), new TaskQueue<Runnable>(), new HashMap(), null);
    }

    @Test
    public void testHostCpuMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostCpuMonitorConfiguration("monitor1", "Scope1", 0, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        Thread.sleep(100);
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 2, is(true));

        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostFileSystemMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostFileSystemMonitorConfiguration("monitor1", "Scope1", 0, null, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        invokeFileSystem();

        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));

        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostMemoryMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostMemoryMonitorConfiguration("monitor1", "Scope1", 0, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 1, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostSwapMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostSwapMonitorConfiguration("monitor1", "Scope1", 0, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 2, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostKpiMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostKpiMonitorConfiguration("monitor1", "Scope1", 0, null, "host.kpi", null, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);
        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 2, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostNetworkMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostNetworkMonitorConfiguration("monitor1", "Scope1", 0,
                null, null, true, true, null);
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, true);

        Thread.sleep(100);

        monitor.measure(measurements, 0, 0, true);

        assertThat(measurements.size() >= 2, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostProcessMonitor() throws Throwable {
        MonitorConfiguration configuration = new HostProcessMonitorConfiguration("monitor1", "Scope1", 0,
                null, Arrays.asList("$.exa.filter('*java*',name) || $.exa.filter('*python*',name)"),
                new ExpressionProcessNamingStrategyConfiguration("name"));
        IMonitor monitor = configuration.createMonitor(context);
        monitor.start();

        List<Measurement> measurements = new ArrayList<Measurement>();
        monitor.measure(measurements, 0, 0, false);

        Thread.sleep(100);

        monitor.measure(measurements, 0, 0, false);

        monitor.measure(measurements, 0, 0, false);

        assertThat(measurements.size() >= 2, is(true));
        checkComponents(configuration, measurements);
    }

    @Test
    public void testHostMonitors() throws Throwable {
        List<Measurement> measurements = new ArrayList<Measurement>();
        MonitorConfiguration configuration1 = new HostCpuMonitorConfiguration("monitor1", null, 0, null);
        IMonitor monitor1 = configuration1.createMonitor(context);
        monitor1.start();

        MonitorConfiguration configuration2 = new HostFileSystemMonitorConfiguration("monitor1", null, 0, null, null);
        IMonitor monitor2 = configuration2.createMonitor(context);
        monitor2.start();

        MonitorConfiguration configuration3 = new HostMemoryMonitorConfiguration("monitor1", null, 0, null);
        IMonitor monitor3 = configuration3.createMonitor(context);
        monitor3.start();

        MonitorConfiguration configuration4 = new HostSwapMonitorConfiguration("monitor1", null, 0, null);
        IMonitor monitor4 = configuration4.createMonitor(context);
        monitor4.start();

        MonitorConfiguration configuration5 = new HostKpiMonitorConfiguration("monitor1", null, 0, null, "host.kpi", null, null);
        IMonitor monitor5 = configuration5.createMonitor(context);
        monitor5.start();

        MonitorConfiguration configuration6 = new HostNetworkMonitorConfiguration("monitor1", null, 0,
                null, null, true, true, null);
        IMonitor monitor6 = configuration6.createMonitor(context);
        monitor6.start();

        MonitorConfiguration configuration7 = new HostProcessMonitorConfiguration("monitor1", null, 0,
                null, Arrays.asList("$.exa.filter('*java*',name) || $.exa.filter('*python*',name)"),
                new ExpressionProcessNamingStrategyConfiguration("name"));
        IMonitor monitor7 = configuration7.createMonitor(context);
        monitor7.start();

        monitor1.measure(measurements, 0, 0, true);
        monitor2.measure(measurements, 0, 0, true);
        monitor3.measure(measurements, 0, 0, true);
        monitor4.measure(measurements, 0, 0, true);
        monitor5.measure(measurements, 0, 0, true);
        monitor6.measure(measurements, 0, 0, true);
        monitor7.measure(measurements, 0, 0, true);
        Thread.sleep(1000);
        invokeFileSystem();
        monitor1.measure(measurements, 0, 0, true);
        monitor2.measure(measurements, 0, 0, true);
        monitor3.measure(measurements, 0, 0, true);
        monitor4.measure(measurements, 0, 0, true);
        monitor5.measure(measurements, 0, 0, true);
        monitor6.measure(measurements, 0, 0, true);
        monitor7.measure(measurements, 0, 0, true);

        MeasurementSet set = new MeasurementSet(measurements, null, 1, Times.getCurrentTime(), 0);
        System.out.println(Measurements.toJson(set, true, false));
    }

    private void invokeFileSystem() throws Throwable {
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);

            File file = File.createTempFile("test", "test");
            file.deleteOnExit();
            FileOutputStream out = new FileOutputStream(file);
            out.write(new byte[1000]);
            out.getFD().sync();
            out.close();

            FileInputStream in = new FileInputStream(file);
            in.read(new byte[1000]);
            in.getFD().sync();
            in.close();
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
