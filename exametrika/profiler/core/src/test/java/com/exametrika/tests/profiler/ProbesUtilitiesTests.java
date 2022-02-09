/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.instrument.StaticJoinPointProvider;
import com.exametrika.impl.profiler.probes.AppStackCounterProvider;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.Probes;
import com.exametrika.spi.profiler.TraceTag;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;


/**
 * The {@link ProbesUtilitiesTests} are tests for {@link Probes} class.
 *
 * @author Medvedev-A
 */
public class ProbesUtilitiesTests {
    private ProbeContext context = createProbeContext();

    @After
    public void tearDown() {
        context.getThreadLocalAccessor().close();
    }

    @Test
    @Ignore
    public void testTimes() {
        assertThat(Times.getTickFrequency() > 0, is(true));
        assertThat(Times.getTickCount() > 0, is(true));
        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            Times.getTickCount();
        System.out.println("Tick count (10^6): " + (System.currentTimeMillis() - t));
        assertThat(Times.getThreadCpuTime() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            Times.getThreadCpuTime();
        System.out.println("Thread cpu time (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testProbes() {
        assertThat(Probes.isInstanceOf(new String(), String.class.getName()), is(true));
        assertThat(Probes.isInstanceOf(new String(), CharSequence.class.getName()), is(true));
        assertThat(Probes.isInstanceOf(new String(), Object.class.getName()), is(true));
        assertThat(Probes.isInstanceOf(new String(), StringBuilder.class.getName()), is(false));

        Exception exception = null;

        try {
            recursion(0);
        } catch (Exception e) {
            exception = new RuntimeException(e.getMessage(), e);
        }

        Json json = Json.object();
        Meters.buildExceptionStackTrace(exception, 3, 5, json, true);
        JsonObject object = json.toObject();
        assertThat((String) object.get("class"), is(RuntimeException.class.getName()));
        assertThat((String) object.get("message"), is("test2..."));
        assertThat(((List) object.get("stackTrace")).size(), is(4));
        assertThat((String) object.select("cause.message"), is("test2..."));
        assertThat(((List) object.select("cause.stackTrace")).size(), is(4));
        assertThat((String) object.select("cause.class"), is(IllegalArgumentException.class.getName()));

        json = Json.object();
        Meters.buildExceptionStackTrace(exception, 3, -5, json, true);
        object = json.toObject();
        assertThat((String) object.get("message"), is("...qqqqq"));
        assertThat(((List) object.get("stackTrace")).size(), is(4));
        assertThat((String) object.select("cause.message"), is("...qqqqq"));
        assertThat(((List) object.select("cause.stackTrace")).size(), is(4));
    }

    @Test
    public void testTraceTag() {
        TraceTag tag = new TraceTag(UUID.randomUUID().toString(), UUID.randomUUID(), 123, System.currentTimeMillis(), 10);
        String str = tag.toString();
        TraceTag tag2 = TraceTag.fromString(str);

        assertThat(tag2.combineId, is(tag.combineId));
        assertThat(tag2.stackId, is(tag.stackId));
        assertThat(tag2.transactionId, is(tag.transactionId));
        assertThat(tag2.transactionStartTime, is(tag.transactionStartTime));
        assertThat(tag2.variant, is(tag.variant));
    }

    @Test
    public void testAppStackCounterProvider() throws Throwable {
        context.getThreadLocalAccessor().get().counters[AppStackCounterType.ALLOCATION_COUNT.ordinal()] = 123;
        AppStackCounterProvider provider = new AppStackCounterProvider(AppStackCounterType.ALLOCATION_COUNT, context);
        assertThat((Long) provider.getValue(), is(123l));

        provider = new AppStackCounterProvider(AppStackCounterType.WALL_TIME, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Wall time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.SYS_TIME, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Sys time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.USER_TIME, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("User time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.WAIT_TIME, context);
        Threads.sleep(10);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Wait time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.WAIT_COUNT, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Wait count (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.BLOCK_TIME, context);
        final Object sync = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (sync) {
                    Threads.sleep(500);
                }

            }
        });
        thread.start();
        Threads.sleep(100);
        synchronized (sync) {
        }
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Block time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.BLOCK_COUNT, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Block count (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.GARBAGE_COLLECTION_TIME, context);
        System.gc();
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Gc time (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.GARBAGE_COLLECTION_COUNT, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Gc count (10^6): " + (System.currentTimeMillis() - t));

        provider = new AppStackCounterProvider(AppStackCounterType.ALLOCATION_BYTES, context);
        assertThat((Long) provider.getValue() > 0, is(true));
        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println("Allocation bytes (10^6): " + (System.currentTimeMillis() - t));
    }

    private void recursion(int count) {
        if (count == 10)
            throw new IllegalArgumentException("test2. qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq");
        else
            recursion(count + 1);
    }

    private ProbeContext createProbeContext() {
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);
        IJoinPointProvider joinPointProvider = new StaticJoinPointProvider(java.util.Collections.<IJoinPoint>emptyList());
        ITimeService timeService = new SystemTimeService();
        IMeasurementHandler measurementHandler = new TestMeasurementHandler();
        MeasurementStrategyManager measurementStrategyManager = new MeasurementStrategyManager();

        ThreadLocalAccessor threadLocalAccessor = new ThreadLocalAccessor(configuration, null, joinPointProvider, null, timeService,
                measurementHandler, measurementStrategyManager, new HashMap(), null, true);

        return new ProbeContext(threadLocalAccessor, null, joinPointProvider, null, timeService, measurementHandler,
                configuration, new HashMap(), measurementStrategyManager);
    }
}
