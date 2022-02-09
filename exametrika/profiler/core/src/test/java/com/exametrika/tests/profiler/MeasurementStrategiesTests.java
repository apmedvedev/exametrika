/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;

import org.junit.Test;

import com.exametrika.api.profiler.IExternalMeasurementStrategy;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighCpuMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighMemoryMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.profiler.strategies.ExternalMeasurementStrategy;
import com.exametrika.impl.profiler.strategies.HighCpuMeasurementStrategy;
import com.exametrika.impl.profiler.strategies.HighMemoryMeasurementStrategy;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link MeasurementStrategiesTests} are tests for measurement strategies.
 *
 * @author Medvedev-A
 */
public class MeasurementStrategiesTests {
    @Test
    public void testMeasurementStrategyManager() {
        ExternalMeasurementStrategyConfiguration strategyConfiguration1 = new ExternalMeasurementStrategyConfiguration("strategy1", true, 0);
        ExternalMeasurementStrategyConfiguration strategyConfiguration2 = new ExternalMeasurementStrategyConfiguration("strategy2", true, 0);
        ExternalMeasurementStrategyConfiguration strategyConfiguration3 = new ExternalMeasurementStrategyConfiguration("strategy3", true, 0);

        MeasurementStrategyManager manager = new MeasurementStrategyManager();
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration1, strategyConfiguration2, strategyConfiguration3), Collections.<ScopeConfiguration>emptySet(),
                Collections.<MonitorConfiguration>emptySet(), Collections.<ProbeConfiguration>emptySet(), 1, 1, 1l, 1l, 1l, new File(""),
                100000, Enums.noneOf(DumpType.class), 0, JsonUtils.EMPTY_OBJECT, null);

        manager.setConfiguration(configuration);
        IExternalMeasurementStrategy strategy1 = manager.findMeasurementStrategy("strategy1");
        strategy1.setAllowed(true);
        assertThat(strategy1.allow(), is(true));

        IExternalMeasurementStrategy strategy2 = manager.findMeasurementStrategy("strategy2");
        IExternalMeasurementStrategy strategy3 = manager.findMeasurementStrategy("strategy3");
        assertThat(strategy2 != null, is(true));
        assertThat(strategy3 != null, is(true));
        assertThat(manager.findMeasurementStrategy("strategy4"), nullValue());

        TestMeasurementStrategyConfiguration strategyConfiguration22 = new TestMeasurementStrategyConfiguration("strategy2");
        ExternalMeasurementStrategyConfiguration strategyConfiguration4 = new ExternalMeasurementStrategyConfiguration("strategy4", true, 0);

        configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, com.exametrika.common.utils.Collections.asSet(
                strategyConfiguration22, strategyConfiguration3, strategyConfiguration4), Collections.<ScopeConfiguration>emptySet(),
                Collections.<MonitorConfiguration>emptySet(), Collections.<ProbeConfiguration>emptySet(), 1, 1, 1l, 1l, 1l, new File(""), 100000,
                Enums.noneOf(DumpType.class), 0, JsonUtils.EMPTY_OBJECT, null);
        manager.setConfiguration(configuration);

        assertThat(manager.findMeasurementStrategy("strategy1"), nullValue());
        assertThat(manager.findMeasurementStrategy("strategy2") != strategy2, is(true));
        assertThat(manager.findMeasurementStrategy("strategy2") instanceof ExternalMeasurementStrategy, is(false));
        assertThat(manager.findMeasurementStrategy("strategy3") == strategy3, is(true));
        assertThat(manager.findMeasurementStrategy("strategy4") instanceof ExternalMeasurementStrategy, is(true));

        manager.setConfiguration(null);

        assertThat(manager.findMeasurementStrategy("strategy1"), nullValue());
        assertThat(manager.findMeasurementStrategy("strategy2"), nullValue());
        assertThat(manager.findMeasurementStrategy("strategy3"), nullValue());
        assertThat(manager.findMeasurementStrategy("strategy4"), nullValue());
    }

    @Test
    public void testHighCpuMeasurementStrategy() {
        HighCpuMeasurementStrategyConfiguration configuration = new HighCpuMeasurementStrategyConfiguration("test", 10, 0);
        HighCpuMeasurementStrategy strategy = (HighCpuMeasurementStrategy) configuration.createStrategy();
        Threads.sleep(10);
        strategy.onTimer();
        assertThat(strategy.allow(), is(false));
        Threads.sleep(1500);
        strategy.onTimer();
        assertThat(strategy.allow(), is(true));
    }

    @Test
    public void testHighMemoryMeasurementStrategy() {
        HighMemoryMeasurementStrategyConfiguration configuration = new HighMemoryMeasurementStrategyConfiguration("test", 10, 0);
        HighMemoryMeasurementStrategy strategy = (HighMemoryMeasurementStrategy) configuration.createStrategy();
        Threads.sleep(10);
        strategy.onTimer();
        assertThat(strategy.allow(), is(false));
        Threads.sleep(1500);
        strategy.onTimer();
        assertThat(strategy.allow(), is(true));
    }

    public static class TestMeasurementStrategyConfiguration extends MeasurementStrategyConfiguration {
        public TestMeasurementStrategyConfiguration(String name) {
            super(name);
        }

        @Override
        public IMeasurementStrategy createStrategy() {
            return new IMeasurementStrategy() {
                @Override
                public boolean allow() {
                    return true;
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestMeasurementStrategyConfiguration))
                return false;

            TestMeasurementStrategyConfiguration configuration = (TestMeasurementStrategyConfiguration) o;
            return super.equals(configuration);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode();
        }
    }
}
