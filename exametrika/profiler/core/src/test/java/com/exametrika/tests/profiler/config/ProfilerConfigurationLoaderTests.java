/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.config;

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

import com.exametrika.api.aggregator.common.meters.config.CountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.CustomHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StatisticsFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.profiler.config.AllocationProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.CheckPointMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration.Type;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExceptionProbeConfiguration;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighCpuMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighMemoryMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.MonitorSetConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.api.profiler.config.ThreadEntryPointProbeConfiguration;
import com.exametrika.api.profiler.config.ThreadExitPointProbeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.profiler.config.ProfilerConfigurationLoader;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration.PrimaryType;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;

/**
 * The {@link ProfilerConfigurationLoaderTests} are tests for {@link ProfilerConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see ProfilerConfigurationLoader
 */
@Ignore
public class ProfilerConfigurationLoaderTests {
    public static class TestProfilerConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.agent", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/agent-extension.json", false));
            TestProfilerProcessor processor = new TestProfilerProcessor();
            parameters.typeLoaders.put("TestFields", processor);
            parameters.typeLoaders.put("TestMonitor", processor);
            parameters.typeLoaders.put("TestMeasurementStrategy", processor);
            parameters.typeLoaders.put("MeasurementsGeneratorMonitor", processor);
            return parameters;
        }
    }

    @Test
    public void testMetricsConfigurationLoad() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("com.exametrika.home", tmpDir);
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        ILoadContext loadContext = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf");
        ProfilerConfiguration configuration = loadContext.get(ProfilerConfiguration.SCHEMA);

        ProbeConfiguration probeConfiguration1 = new StackProbeConfiguration("stack", "test", 2000, "strategy1", 123,
                Arrays.asList(new StandardFieldConfiguration()), Arrays.asList(new AppStackCounterConfiguration(false,
                AppStackCounterType.WALL_TIME)), new GaugeConfiguration(true, Arrays.asList(new StandardFieldConfiguration())),
                1001, 20000, 1003, 1004, 1005, 77, 11, 200, 127, 3, 10000, CombineType.STACK, null);
        ProbeConfiguration probeConfiguration2 = new ExceptionProbeConfiguration("exception", "default", 10000, null, 60000,
                new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration3 = new AllocationProbeConfiguration("allocation", "default", 1000, null, 60000);
        ProbeConfiguration probeConfiguration5 = new ThreadEntryPointProbeConfiguration("threadEntryPoint", "default", null, 60000, 2,
                new CounterConfiguration(true, false, 0), new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100),
                PrimaryType.NO, true, new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                new CounterConfiguration(true, false, 0), new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 10, 100));
        ProbeConfiguration probeConfiguration6 = new ThreadExitPointProbeConfiguration("threadExitPoint", "default", null, 60000);

        ProfilerConfiguration configuration2 = new ProfilerConfiguration("node", TimeSource.THREAD_CPU_TIME, Collections.asSet(new ExternalMeasurementStrategyConfiguration("strategy1", true, 0),
                new TestMeasurementStrategyConfiguration("strategy2"), new CheckPointMeasurementStrategyConfiguration("strategy3", false),
                new HighCpuMeasurementStrategyConfiguration("strategy4", 10, 11), new HighMemoryMeasurementStrategyConfiguration("strategy5",
                        10, 11), new CompositeMeasurementStrategyConfiguration("strategy6", false, Type.OR, Arrays.asList(
                        new ExternalMeasurementStrategyConfiguration("s1", true, 60000), new ExternalMeasurementStrategyConfiguration("s2", true, 60000)))), Collections.asSet(new ScopeConfiguration("scope1", "scope1", "scopeType", "test*"),
                new ScopeConfiguration("scope2", "scope2", "default", null)),
                Collections.asSet(
                        new MonitorSetConfiguration("monitor1", 100, "strategy1", Collections.asSet(new TestMonitorConfiguration("monitor2",
                                "scope2", 200, null, new GaugeConfiguration(false)))),
                        new TestMonitorConfiguration("monitor3", "scope3", 400, "strategy1", new GaugeConfiguration(false,
                                Arrays.asList(new StandardFieldConfiguration(), new StatisticsFieldConfiguration(),
                                        new UniformHistogramFieldConfiguration(10, 100, 10), new LogarithmicHistogramFieldConfiguration(10, 10),
                                        new CustomHistogramFieldConfiguration(Arrays.asList(10l, 20l, 30l, 40l)),
                                        new InstanceFieldConfiguration(10, true)))),
                        new TestMonitorConfiguration("monitor4", "scope4", 400, null, new CounterConfiguration(false, true, 0)),
                        new TestMonitorConfiguration("monitor5", "scope5", 400, "strategy1", new LogConfiguration(false,
                                new ExpressionLogFilterConfiguration("value != null"), Arrays.<LogMeterConfiguration>asList(
                                new LogMeterConfiguration("meter1", new CounterConfiguration(true, false, 0),
                                        new ExpressionLogFilterConfiguration("testFilter"), new CountLogProviderConfiguration()),
                                new LogMeterConfiguration("meter2", new GaugeConfiguration(true), null, null)),
                                new ExpressionLogFilterConfiguration("value != null"), new ExpressionLogProviderConfiguration("new JsonObjectBuilder(value).toJson()"), 1000, 1024, 20000, 100, 100)),
                        new TestMonitorConfiguration("monitor6", "scope6", 400, null, new InfoConfiguration(false))
                ), Collections.asSet(probeConfiguration1, probeConfiguration2, probeConfiguration3,
                probeConfiguration5, probeConfiguration6), 2, 4, 200, 20000, 20000, new File(tmpDir, "/work/profiler"), 200000,
                Enums.of(DumpType.STATE, DumpType.FULL_STATE, DumpType.MEASUREMENTS), 120000,
                JsonUtils.EMPTY_OBJECT, null);

        assertThat(sortProbes(configuration2.getProbes()), is(sortProbes(configuration.getProbes())));
        assertThat(sortMonitors(configuration2.getMonitors()), is(sortMonitors(configuration.getMonitors())));
        assertThat(configuration2, is(configuration));
    }

    private List<ProbeConfiguration> sortProbes(Set<ProbeConfiguration> probes) {
        List<ProbeConfiguration> list = new ArrayList<ProbeConfiguration>(probes);
        java.util.Collections.sort(list, new Comparator<ProbeConfiguration>() {
            @Override
            public int compare(ProbeConfiguration o1, ProbeConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return list;
    }

    private List<MonitorConfiguration> sortMonitors(Set<MonitorConfiguration> monitors) {
        List<MonitorConfiguration> list = new ArrayList<MonitorConfiguration>(monitors);
        java.util.Collections.sort(list, new Comparator<MonitorConfiguration>() {
            @Override
            public int compare(MonitorConfiguration o1, MonitorConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return list;
    }

    private static String getResourcePath() {
        String className = ProfilerConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }

    private static class TestProfilerProcessor extends AbstractExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            JsonObject element = (JsonObject) object;
            if (element.contains("instanceOf"))
                type = element.get("instanceOf");
            if (type.equals("TestFields"))
                return new TestFieldConfiguration();
            else if (type.equals("TestMonitor")) {
                String scope = (String) element.get("scope", null);
                long period = element.get("period");
                String measurementStrategy = element.get("measurementStrategy", null);
                MeterConfiguration meter = load(null, null, (JsonObject) element.get("meter"), context);
                return new TestMonitorConfiguration(name, scope, period, measurementStrategy, meter);
            } else if (type.equals("TestMeasurementStrategy"))
                return new TestMeasurementStrategyConfiguration(name);
            else
                throw new InvalidConfigurationException();
        }
    }

    private static class TestFieldConfiguration extends FieldConfiguration {
        @Override
        public FieldValueSchemaConfiguration getSchema() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestFieldConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IFieldFactory createFactory() {
            return null;
        }
    }

    private static class TestMonitorConfiguration extends MonitorConfiguration {
        private final MeterConfiguration meter;

        public TestMonitorConfiguration(String name, String scope, long period, String measurementStrategy, MeterConfiguration meter) {
            super(name, scope, period, measurementStrategy);

            this.meter = meter;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestMonitorConfiguration))
                return false;

            TestMonitorConfiguration configuration = (TestMonitorConfiguration) o;
            return super.equals(o) && meter.equals(configuration.meter);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }

        @Override
        public IMonitor createMonitor(IMonitorContext context) {
            return null;
        }

        @Override
        public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        }
    }

    private static class TestMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
        public TestMeasurementStrategyConfiguration(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestMeasurementStrategyConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IMeasurementStrategy createStrategy() {
            return null;
        }
    }
}
