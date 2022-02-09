/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.IProbe;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Out;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.instrument.StaticJoinPointProvider;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;


/**
 * The {@link ThreadLocalAccessorTests} are tests for {@link ThreadLocalAccessor}.
 *
 * @author Medvedev-A
 */
public class ThreadLocalAccessorTests {
    private ThreadLocalAccessor accessor = createAccessor();

    @After
    public void tearDown() {
        Times.clearTest();
        accessor.close();
    }

    @Test
    public void testAccessor() throws Throwable {
        List<Thread> threads = new ArrayList<Thread>();
        List<TestRunnable> runnables = new ArrayList<TestRunnable>();
        for (int i = 0; i < 100; i++) {
            TestRunnable runnable = new TestRunnable();
            runnables.add(runnable);
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        Thread.sleep(500);

        List<Container> containers = Tests.get(accessor, "containers");
        assertTrue(containers.size() == 100);

        for (int i = 0; i < 50; i++)
            runnables.get(i).stop = true;
        for (int i = 0; i < 50; i++)
            threads.get(i).join();

        accessor.onTimer();
        assertTrue(containers.size() == 50);

        accessor.close();

        assertTrue(containers.isEmpty());

        for (int i = 50; i < 100; i++)
            threads.get(i).join();
    }

    @Test
    public void testPerformance() throws Throwable {
        new Benchmark<IProbe>(new Probe() {
            @Override
            public long run() {
                long l = 0;
                for (int i = 0; i < 1000000000; i++) {
                    accessor.get();
                    l++;
                }

                return l;
            }
        }).print("Thread local accessor - ");
    }

    @Test
    public void testThreadTargetChange() throws Throwable {
        final Out<Boolean> res = new Out<Boolean>(false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                res.value = true;
            }
        });
        final Runnable oldTarget = ThreadLocalAccessor.getThreadLocal(thread);
        final Out<Boolean> intercepted = new Out<Boolean>(false);
        ThreadLocalAccessor.setThreadLocal(thread, new Runnable() {
            @Override
            public void run() {
                intercepted.value = true;
                oldTarget.run();
            }
        });

        thread.start();
        thread.join();

        assertThat(res.value, is(true));
        assertThat(intercepted.value, is(true));
    }

    @Test
    public void testContainerRemove() throws Throwable {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                accessor.get();
                try {
                    List<Container> containers = Tests.get(accessor, "containers");
                    assertTrue(containers.size() == 1);
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
        thread.start();
        thread.join();

        accessor.onTimer();

        List<Container> containers = Tests.get(accessor, "containers");
        assertTrue(containers.isEmpty());
    }

    private ThreadLocalAccessor createAccessor() {
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);
        IJoinPointProvider joinPointProvider = new StaticJoinPointProvider(java.util.Collections.<IJoinPoint>emptyList());
        ITimeService timeService = new SystemTimeService();
        IMeasurementHandler measurementHandler = new TestMeasurementHandler();
        MeasurementStrategyManager measurementStrategyManager = new MeasurementStrategyManager();

        return new ThreadLocalAccessor(configuration, null, joinPointProvider, null, timeService,
                measurementHandler, measurementStrategyManager, new HashMap(), null, true);
    }

    private class TestRunnable implements Runnable {
        private boolean stop;

        @Override
        public void run() {
            Container container = accessor.get();
            Container container2 = accessor.get();
            assertTrue(container2 == container);
            assertTrue(container.thread == Thread.currentThread());

            while (!stop && !accessor.isClosed()) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        }
    }
}
