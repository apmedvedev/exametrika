/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.MonitorSetConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.profiler.monitors.MonitorManager;
import com.exametrika.impl.profiler.strategies.ExternalMeasurementStrategy;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.MeasurementStrategiesTests.TestMeasurementStrategyConfiguration;
import com.exametrika.tests.profiler.support.TestTimeService;


/**
 * The {@link MonitorsTests} are tests for monitors.
 *
 * @author Medvedev-A
 */
public class MonitorsTests {
    @Test
    public void testManagerChangeConfiguration() throws Throwable {
        ExternalMeasurementStrategyConfiguration strategyConfiguration1 = new ExternalMeasurementStrategyConfiguration("strategy1", true, 0);
        ExternalMeasurementStrategyConfiguration strategyConfiguration2 = new ExternalMeasurementStrategyConfiguration("strategy2", true, 0);
        ExternalMeasurementStrategyConfiguration strategyConfiguration3 = new ExternalMeasurementStrategyConfiguration("strategy3", true, 0);

        TestMonitorConfiguration monitorConfiguration1 = new TestMonitorConfiguration("monitor1", "scope1", 10, "strategy2");
        TestMonitorConfiguration monitorConfiguration2 = new TestMonitorConfiguration("monitor2", "scope2", 10, "strategy2");
        TestMonitorConfiguration monitorConfiguration3 = new TestMonitorConfiguration("monitor3", "scope3", 10, "strategy3");

        MeasurementStrategyManager strategyManager = new MeasurementStrategyManager();
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration1, strategyConfiguration2, strategyConfiguration3), Collections.<ScopeConfiguration>emptySet(),
                com.exametrika.common.utils.Collections.asSet(monitorConfiguration1, monitorConfiguration2, monitorConfiguration3),
                Collections.<ProbeConfiguration>emptySet(), 1, 1, 1l, 100l, 1l, new File(""), 100000, Enums.noneOf(DumpType.class), 0,
                JsonUtils.EMPTY_OBJECT, null);

        strategyManager.setConfiguration(configuration);

        TestTimeService timeService = new TestTimeService();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        MonitorManager manager = new MonitorManager(strategyManager, handler, timeService, new HashMap(), null);
        manager.start();

        manager.setConfiguration(configuration);

        Map<String, TestMonitor> monitors = getMonitors(manager, strategyManager);
        assertThat(monitors.keySet(), is(com.exametrika.common.utils.Collections.asSet("monitor1", "monitor2", "monitor3")));

        TestMeasurementStrategyConfiguration strategyConfiguration22 = new TestMeasurementStrategyConfiguration("strategy2");
        TestMonitorConfiguration monitorConfiguration22 = new TestMonitorConfiguration("monitor2", "scope2", 1, "strategy2");
        TestMonitorConfiguration monitorConfiguration4 = new TestMonitorConfiguration("monitor4", "scope4", 10, "strategy3");

        ProfilerConfiguration configuration2 = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration22, strategyConfiguration3), Collections.<ScopeConfiguration>emptySet(),
                com.exametrika.common.utils.Collections.asSet(monitorConfiguration1, monitorConfiguration22, monitorConfiguration3,
                        monitorConfiguration4), Collections.<ProbeConfiguration>emptySet(),
                1, 1, 1l, 100l, 1l, new File(""), 100000, Enums.noneOf(DumpType.class), 0, JsonUtils.EMPTY_OBJECT, null);

        IMeasurementStrategy strategy2 = monitors.get("monitor1").strategy;
        IMeasurementStrategy strategy3 = monitors.get("monitor3").strategy;
        strategyManager.setConfiguration(configuration2);
        manager.setConfiguration(configuration2);

        Map<String, TestMonitor> monitors2 = getMonitors(manager, strategyManager);
        assertThat(monitors2.keySet(), is(com.exametrika.common.utils.Collections.asSet("monitor1", "monitor2", "monitor3", "monitor4")));
        assertThat(monitors2.get("monitor1") == monitors.get("monitor1"), is(true));
        assertThat(monitors2.get("monitor1").strategy != strategy2, is(true));
        assertThat(monitors2.get("monitor2") != monitors.get("monitor2"), is(true));
        assertThat(monitors2.get("monitor3") == monitors.get("monitor3"), is(true));
        assertThat(monitors2.get("monitor3").strategy == strategy3, is(true));

        ProfilerConfiguration configuration3 = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration22, strategyConfiguration3), Collections.<ScopeConfiguration>emptySet(),
                com.exametrika.common.utils.Collections.asSet(monitorConfiguration22, monitorConfiguration3,
                        monitorConfiguration4), Collections.<ProbeConfiguration>emptySet(),
                1, 1, 1l, 100l, 1l, new File(""), 100000, Enums.noneOf(DumpType.class), 0, JsonUtils.EMPTY_OBJECT, null);

        manager.setConfiguration(configuration3);

        Map<String, TestMonitor> monitors3 = getMonitors(manager, strategyManager);
        assertThat(monitors3.keySet(), is(com.exametrika.common.utils.Collections.asSet("monitor2", "monitor3", "monitor4")));

        assertThat(monitors2.get("monitor1").stopped, is(true));

        manager.stop();
        assertThat(getMonitors(manager, strategyManager).isEmpty(), is(true));

        for (TestMonitor monitor : monitors3.values())
            assertThat(monitor.stopped, is(true));
    }

    @Test
    public void testManagerMeasure() throws Throwable {
        ExternalMeasurementStrategyConfiguration strategyConfiguration1 = new ExternalMeasurementStrategyConfiguration("strategy1", true, 0);

        TestMonitorConfiguration monitorConfiguration1 = new TestMonitorConfiguration("monitor1", "scope1", 200, "strategy1");
        TestMonitorConfiguration monitorConfiguration2 = new TestMonitorConfiguration("monitor2", "scope1", 200, null);

        MeasurementStrategyManager strategyManager = new MeasurementStrategyManager();
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration1), Collections.<ScopeConfiguration>emptySet(),
                com.exametrika.common.utils.Collections.asSet(monitorConfiguration1, monitorConfiguration2),
                Collections.<ProbeConfiguration>emptySet(),
                1, 1, 10l, 1000l, 1l, new File(""), 100000, Enums.noneOf(DumpType.class), 0, JsonUtils.EMPTY_OBJECT, null);

        strategyManager.setConfiguration(configuration);

        TestTimeService timeService = new TestTimeService();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        MonitorManager manager = new MonitorManager(strategyManager, handler, timeService, new HashMap(), null);
        manager.start();

        manager.setConfiguration(configuration);

        TestMonitor monitor1 = getMonitors(manager, strategyManager).get("monitor1");
        TestMonitor monitor2 = getMonitors(manager, strategyManager).get("monitor2");

        assertThat(monitor1.measured, is(false));
        assertThat(monitor1.force, is(false));
        assertThat(monitor2.measured, is(false));
        assertThat(monitor2.force, is(false));

        timeService.time = 150;
        Thread.sleep(200);

        assertThat(monitor1.measured, is(false));
        assertThat(monitor1.force, is(false));

        timeService.time = 200;
        Thread.sleep(200);

        assertThat(monitor1.measured, is(true));
        assertThat(monitor1.force, is(false));

        timeService.time = 1000;
        Thread.sleep(200);
        timeService.time = 2000;
        Thread.sleep(200);

        assertThat(monitor1.measured, is(true));
        assertThat(monitor1.force, is(true));
        assertThat(monitor2.measured, is(true));
        assertThat(monitor2.force, is(true));
        monitor1.measured = false;
        monitor1.force = false;
        monitor2.measured = false;
        monitor2.force = false;

        Thread.sleep(200);

        assertThat(monitor1.measured, is(false));
        assertThat(monitor1.force, is(false));

        ((ExternalMeasurementStrategy) monitor1.strategy).setAllowed(false);
        timeService.time = 3000;

        Thread.sleep(200);

        assertThat(monitor1.measured, is(false));
        assertThat(monitor1.force, is(false));
        assertThat(monitor2.measured, is(true));
        assertThat(monitor2.force, is(true));

        ((ExternalMeasurementStrategy) monitor1.strategy).setAllowed(true);
        Thread.sleep(200);

        assertThat(monitor1.measured, is(true));
        assertThat(monitor1.force, is(false));
        manager.stop();
    }

    private Map<String, TestMonitor> getMonitors(MonitorManager manager, MeasurementStrategyManager strategyManager) throws Throwable {
        Map<String, TestMonitor> map = new HashMap<String, TestMonitor>();
        List monitors = Tests.get(manager, "monitors");

        for (Object info : monitors) {
            TestMonitor monitor = Tests.get(info, "monitor");
            assertThat(monitor.started, is(true));
            assertThat(!monitor.stopped, is(true));
            assertThat(monitor.context != null, is(true));
            map.put(monitor.configuration.getName(), monitor);
            monitor.strategy = Tests.get(info, "measurementStrategy");
            assertThat((monitor.strategy != null) == (monitor.configuration.getMeasurementStrategy() != null), is(true));
            if (monitor.strategy != null)
                assertThat(monitor.strategy == strategyManager.findMeasurementStrategy(monitor.configuration.getMeasurementStrategy()), is(true));
        }

        return map;
    }

    public static class TestMeasurementHandler implements IMeasurementHandler {
        public List<MeasurementSet> measurements = new ArrayList<MeasurementSet>();

        @Override
        public void handle(MeasurementSet measurement) {
            measurements.add(measurement);
        }

        @Override
        public boolean canHandle() {
            return true;
        }
    }

    private static class TestMonitor implements IMonitor {
        private boolean started;
        private boolean stopped;
        private boolean measured;
        private boolean force;
        private final TestMonitorConfiguration configuration;
        private final IMonitorContext context;
        private IMeasurementStrategy strategy;

        public TestMonitor(TestMonitorConfiguration configuration, IMonitorContext context) {
            this.configuration = configuration;
            this.context = context;
        }

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void measure(List<Measurement> measurements, long time, long period, boolean force) {
            measured = true;
            if (force)
                this.force = true;
        }
    }

    private static class TestMonitorConfiguration extends MonitorConfiguration {
        public TestMonitorConfiguration(String name, String scope, long period, String measurementStrategy) {
            super(name, scope, period, measurementStrategy);
        }

        @Override
        public IMonitor createMonitor(IMonitorContext context) {
            return new TestMonitor(this, context);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof MonitorSetConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }

        @Override
        public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        }
    }
}
