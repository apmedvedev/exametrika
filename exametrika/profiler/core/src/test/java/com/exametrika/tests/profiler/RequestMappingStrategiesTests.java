/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.CompositeRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.HotspotRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.SimpleRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.ThresholdRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.meters.StandardExpressionContext;
import com.exametrika.impl.instrument.StaticJoinPointProvider;
import com.exametrika.impl.profiler.probes.CompositeRequestMappingStrategy;
import com.exametrika.impl.profiler.probes.HotspotRequestMappingStrategy;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.probes.ThresholdRequestMappingStrategy;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestThreadLocalRegistry;
import com.exametrika.tests.profiler.support.TestThreadLocalSlot;


/**
 * The {@link RequestMappingStrategiesTests} are tests for request mapping strategies.
 *
 * @author Medvedev-A
 */
@Ignore
public class RequestMappingStrategiesTests {
    private ProbeContext context;

    @Before
    public void setUp() {
        context = createProbeContext();
    }

    @After
    public void tearDown() {
        Times.clearTest();
        context.getThreadLocalAccessor().close();
    }

    @Test
    public void testExpressionContext() {
        String hidden = new StandardExpressionContext().hide("Hello world!!!");
        assertThat(hidden, is("##87ee732d831690f45b8606b1547bd09e"));

        assertThat(new StandardExpressionContext().filter("Hell*", "Hello world"), is(true));
        assertThat(new StandardExpressionContext().filter("Hell*", "world"), is(false));
    }

    @Test
    public void testSimpleRequestMappingStrategy() {
        SimpleRequestMappingStrategyConfiguration configuration = new SimpleRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null);
        Request r = new Request();
        r.name = "test1";
        r.f1 = "field1";
        r.f2 = 10;

        IRequestMappingStrategy strategy = configuration.createStrategy(context);
        IRequest request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(true));
        assertThat(request.getName(), is("test1"));
        assertThat(request.getMetadata(), is(Json.object().put("name", "test1").put("f1", "field1").toObject()));
        assertThat(request.getParameters(), is(Json.object().put("name", new StandardExpressionContext().hide("test1")).put("f1", "fi...").put("f2", 10).toObject()));

        configuration = new SimpleRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':name, 'f1':f1, 'f2':10}", "$.exa.filter('name*', name)");
        strategy = configuration.createStrategy(context);
        r = new Request();
        r.name = "test1";
        assertThat(strategy.begin(null, r), nullValue());

        r = new Request();
        r.name = "name1";
        assertThat(strategy.begin(null, r) != null, is(true));
    }

    @Test
    public void testSimpleRequestMappingStrategyPerformance() {
        SimpleRequestMappingStrategyConfiguration configuration = new SimpleRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null);

        IRequestMappingStrategy strategy = configuration.createStrategy(context);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Simple request mapping strategy performance (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testTimeThresholdRequestMappingStrategy() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        ThresholdRequestMappingStrategyConfiguration configuration = new ThresholdRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null,
                1000000000, 1000, 1000, 10, 50);
        ThresholdRequestMappingStrategy strategy = (ThresholdRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);

        strategy.onTimer(1000);

        for (int i = 0; i < 100; i++) {
            Request r = new Request();
            r.name = "test" + (i % 10);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest(100000000);
            assertThat(request.canMeasure(), is(false));
            request.end();
        }

        strategy.onTimer(2000);

        for (int i = 0; i < 100; i++) {
            Request r = new Request();
            r.name = "test" + (i % 10);
            r.f1 = "field1";
            r.f2 = 10;

            assertThat(strategy.begin(null, r), nullValue());
        }

        strategy.onTimer(3000);

        for (int i = 0; i < 100; i++) {
            Request r = new Request();
            r.name = "test" + (i % 10);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest(1000000000);
            if (i < 10)
                assertThat(request.canMeasure(), is(false));
            else
                assertThat(request.canMeasure(), is(true));

            request.end();
        }

        strategy.onTimer(4000);

        for (int i = 0; i < 100; i++) {
            Request r = new Request();
            r.name = "test" + (i % 10);
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }

        strategy.onTimer(5000);

        for (int i = 0; i < 110; i++) {
            Request r = new Request();
            r.name = "test" + (i % 10);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest(100000000);
            if (i < 100)
                assertThat(request.canMeasure(), is(true));
            else
                assertThat(request.canMeasure(), is(false));

            request.end();
        }

        for (int i = 0; i < 100; i++) {
            Request r = new Request();
            r.name = "test" + (i % 20);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest(1000000000);

            assertThat(request.canMeasure(), is(false));

            request.end();
        }
    }

    @Test
    public void testCountersThresholdRequestMappingStrategy() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        ThresholdRequestMappingStrategyConfiguration configuration = new ThresholdRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, "$.exa.counters.counter($.exa.counters.waitTime)",
                "$.exa.counters.counter($.exa.counters.waitTime)", 100, 0, 0, 10, 50);
        ThresholdRequestMappingStrategy strategy = (ThresholdRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);

        Request r = new Request();
        r.name = "test1";

        IRequest request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(false));
        request.end();

        request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(false));
        Threads.sleep(200);
        request.end();

        request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(true));
        request.end();
    }

    @Test
    public void testRequestThresholdRequestMappingStrategy() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        ThresholdRequestMappingStrategyConfiguration configuration = new ThresholdRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, "f2", 100, 0, 0, 10, 50);
        ThresholdRequestMappingStrategy strategy = (ThresholdRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);

        Request r = new Request();
        r.name = "test1";
        r.f2 = 10;

        IRequest request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(false));
        request.end();

        r.f2 = 100;
        request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(false));
        request.end();

        request = strategy.begin(null, r);
        assertThat(request.canMeasure(), is(true));
        request.end();
    }

    @Test
    public void testTimeThresholdRequestMappingStrategyPerformance() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        ThresholdRequestMappingStrategyConfiguration configuration = new ThresholdRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null, 100000000,
                1000, 1000, 10, 50);
        ThresholdRequestMappingStrategy strategy = (ThresholdRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);
        Times.clearTest();

        strategy.onTimer(1000);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Time threshold request mapping strategy estimation performance (10^6): " + (System.currentTimeMillis() - t));

        Request r = new Request();
        r.name = "test1";
        r.f1 = "field1";
        r.f2 = 10;

        Times.setTest(0);
        IRequest request = strategy.begin(null, r);
        Times.setTest(1000000000);
        request.end();

        Times.clearTest();

        strategy.onTimer(2000);

        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Time threshold request mapping strategy measurement performance (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testRequestThresholdRequestMappingStrategyPerformance() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        ThresholdRequestMappingStrategyConfiguration configuration = new ThresholdRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, "f2", 100, 0, 0, 10, 50);
        ThresholdRequestMappingStrategy strategy = (ThresholdRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);
        Times.clearTest();

        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Request threshold request mapping strategy performance (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testTimeHotspotRequestMappingStrategy() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        HotspotRequestMappingStrategyConfiguration configuration = new HotspotRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null, 1000, 1000, 10, 10,
                1, 100, 10000, null, false);
        HotspotRequestMappingStrategy strategy = (HotspotRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);

        strategy.onTimer(1000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest((i % 100) * 1000000);
            assertThat(request.canMeasure(), is(false));
            request.end();
        }

        strategy.onTimer(2000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            if ((i % 100) >= 90) {
                assertThat(request.canMeasure(), is(true));
                request.end();
            } else
                assertThat(request, nullValue());
        }

        strategy.onTimer(3000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest((99 - (i % 100)) * 1000000);
            assertThat(request.canMeasure(), is((i % 100) >= 90));
            request.end();
        }

        strategy.onTimer(4000);
        strategy.onTimer(5000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest((99 - (i % 100)) * 1000000);
            assertThat(request.canMeasure(), is((i % 100) < 10));
            request.end();
        }
    }

    @Test
    public void testRequestHotspotRequestMappingStrategy() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        HotspotRequestMappingStrategyConfiguration configuration = new HotspotRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, "f2", 1000, 0, 10, 10,
                1, 100, 10000, null, false);
        HotspotRequestMappingStrategy strategy = (HotspotRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);

        strategy.onTimer(1000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = i % 100;

            IRequest request = strategy.begin(null, r);
            assertThat(request.canMeasure(), is(false));
            request.end();
        }

        strategy.onTimer(2000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 99 - (i % 100);

            IRequest request = strategy.begin(null, r);
            assertThat(request.canMeasure(), is((i % 100) >= 90));
            request.end();
        }

        strategy.onTimer(3000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            assertThat(request.canMeasure(), is((i % 100) < 10));
            request.end();
        }
    }

    @Test
    public void testTimeHotspotRequestMappingStrategyPerformance() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        HotspotRequestMappingStrategyConfiguration configuration = new HotspotRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null, 1000, 1000, 10, 10,
                1, 100, 10000, null, false);
        HotspotRequestMappingStrategy strategy = (HotspotRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);
        Times.clearTest();

        strategy.onTimer(1000);
        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Time hotspot request mapping strategy estimation performance (10^6): " + (System.currentTimeMillis() - t));

        strategy.onTimer(2000);

        t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Time hotspot request mapping strategy measurement performance (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testRequestHotspotRequestMappingStrategyPerformance() {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        HotspotRequestMappingStrategyConfiguration configuration = new HotspotRequestMappingStrategyConfiguration("name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, "f2", 1000, 0, 10, 10,
                1, 100, 10000, null, false);
        HotspotRequestMappingStrategy strategy = (HotspotRequestMappingStrategy) configuration.createStrategy(context);
        slot.value = strategy.allocate();
        strategy.setSlot(slot);
        Times.clearTest();

        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Request r = new Request();
            r.name = "test1";
            r.f1 = "field1";
            r.f2 = 10;

            IRequest request = strategy.begin(null, r);
            request.end();
        }
        System.out.println("Request hotspot request mapping strategy performance (10^6): " + (System.currentTimeMillis() - t));
    }

    @Test
    public void testCompositeRequestMappingStrategy() {
        TestThreadLocalRegistry registry = new TestThreadLocalRegistry();

        ThresholdRequestMappingStrategyConfiguration configuration1 = new ThresholdRequestMappingStrategyConfiguration("'t-' + name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null,
                1000000000, 1000, 0, 100, 50);
        HotspotRequestMappingStrategyConfiguration configuration2 = new HotspotRequestMappingStrategyConfiguration("'h-' + name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null, null, null, 1000, 0, 10, 10,
                1, 100, 10000, null, false);
        SimpleRequestMappingStrategyConfiguration configuration3 = new SimpleRequestMappingStrategyConfiguration("'s-' + name",
                "{'name':name, 'f1':f1}", "{'name':$.exa.hide(name), 'f1':$.exa.truncate(f1, 2, true), 'f2':10}", null);
        CompositeRequestMappingStrategyConfiguration configuration = new CompositeRequestMappingStrategyConfiguration(Arrays.asList(
                configuration1, configuration2, configuration3));

        CompositeRequestMappingStrategy strategy = (CompositeRequestMappingStrategy) configuration.createStrategy(context);
        strategy.register(registry);

        strategy.onTimer(1000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest((i % 100) * 1000000);
            assertThat(request.canMeasure(), is(true));
            assertThat(request.getName().startsWith("s-"), is(true));
            request.end();
        }

        strategy.onTimer(2000);

        for (int i = 0; i < 1000; i++) {
            Request r = new Request();
            r.name = "test" + (i % 100);
            r.f1 = "field1";
            r.f2 = 10;

            Times.setTest(0);
            IRequest request = strategy.begin(null, r);
            Times.setTest(1000000000);

            if (i < 100) {
                if ((i % 100) >= 90)
                    assertThat(request.getName().startsWith("h-"), is(true));
                else
                    assertThat(request.getName().startsWith("s-"), is(true));
            } else
                assertThat(request.getName().startsWith("t-"), is(true));
            assertThat(request.canMeasure(), is(true));
            request.end();
        }
    }

    private ProbeContext createProbeContext() {
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.THREAD_CPU_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);
        IJoinPointProvider joinPointProvider = new StaticJoinPointProvider(java.util.Collections.<IJoinPoint>emptyList());
        ITimeService timeService = new SystemTimeService();
        IMeasurementHandler measurementHandler = new TestMeasurementHandler();
        MeasurementStrategyManager measurementStrategyManager = new MeasurementStrategyManager();

        ThreadLocalAccessor threadLocalAccessor = new ThreadLocalAccessor(configuration, null, joinPointProvider, null, timeService,
                measurementHandler, measurementStrategyManager, new HashMap());

        return new ProbeContext(threadLocalAccessor, null, joinPointProvider, null, timeService, measurementHandler,
                configuration, new HashMap(), measurementStrategyManager);
    }

    public static class Request {
        public String name;
        public String f1;
        public int f2;
    }
}
