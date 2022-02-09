/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.AppStackCounterConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.StackInterceptPointcut;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.StaticJoinPointProvider;
import com.exametrika.impl.profiler.boot.AgentStackProbeInterceptor;
import com.exametrika.impl.profiler.boot.AgentlessStackProbeInterceptor;
import com.exametrika.impl.profiler.probes.AppStackCounterProvider;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.probes.StackProbe;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.perftests.common.lz4.LZ4PerfTests;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;
import com.exametrika.tests.instrument.instrumentors.TestClassLoader;
import com.exametrika.tests.profiler.support.ITestStackClass1;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestStackClass1;
import com.exametrika.tests.profiler.support.TestStackClass3;

/**
 * The {@link StackProbeTests} are tests for {@link StackProbe}.
 *
 * @author Medvedev-A
 */
@Ignore
public class StackProbeTests {
    private InterceptorManager interceptorManager = new InterceptorManager();
    private ITimeService timeService = new SystemTimeService();
    private TestMeasurementHandler measurementHandler = new TestMeasurementHandler();
    private MeasurementStrategyManager measurementStrategyManager = new MeasurementStrategyManager();
    public static ThreadLocalAccessor accessor;
    private File workDir;

    @Before
    public void setUp() {
        Times.clearTest();

        Interceptors.setInvokeDispatcher(interceptorManager);
        workDir = new File(System.getProperty("java.io.tmpdir"), "/profiler/work");
        Files.emptyDir(workDir);

        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(
                new ExternalMeasurementStrategyConfiguration("strategy1", true, 0)),
                Collections.asSet(new ScopeConfiguration("scope1", "scope1", "default", null)), Collections.<MonitorConfiguration>asSet(), Collections.asSet(
                new StackProbeConfiguration("stack", "default", 1000, "strategy1", 0, Arrays.asList(new StandardFieldConfiguration()
                        , new LogarithmicHistogramFieldConfiguration(0, 40)
                ),
                        Arrays.<StackCounterConfiguration>asList(new AppStackCounterConfiguration(true, AppStackCounterType.ALLOCATION_BYTES),
                                new AppStackCounterConfiguration(true, AppStackCounterType.ERRORS_COUNT)),
                        new GaugeConfiguration(false),
                        1000, 20000, 100, 100, 1, 90, 1, 400, 10, 3, 0, CombineType.STACK, null)),
                1, 1, 100, 300000, 300000, workDir, 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null);

        measurementStrategyManager.setConfiguration(configuration);

        accessor = new ThreadLocalAccessor(configuration, null, interceptorManager, null, timeService,
                measurementHandler, measurementStrategyManager, new HashMap());
    }

    @After
    public void tearDown() {
        Times.clearTest();
        accessor.close();
        workDir.delete();
    }

    @Test
    public void testStackProbe() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        Scope[] scopes = Tests.get(accessor.get().scopes, "activeScopes");
        Scope scope1 = scopes[0];
        List<IProbeCollector> collectors = Tests.get(scope1, "collectors");
        final StackProbeRootCollector root = (StackProbeRootCollector) collectors.get(0);

        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(TestStackClass1.class.getName() + "*"),
                new MemberFilter(Arrays.asList(new MemberFilter("*")), Arrays.asList(new MemberFilter("*ultraFast*")/*,
                new MemberFilter("*fast*"), new MemberFilter("*normal*")*/)));
        StackInterceptPointcut pointcut = new StackInterceptPointcut("test", filter, ThreadLocalAccessor.underAgent ?
                AgentStackProbeInterceptor.class : AgentlessStackProbeInterceptor.class);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestStackClass1.class.getName(), classTransformer);

        Class clazz = classLoader.loadClass(TestStackClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestStackClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        System.out.println("----------------------------------");
        System.out.println(root.dump(IProfilerMXBean.STATE_FLAG));

        System.out.println("----------------------------------");
        final TestStackClass1 original = new TestStackClass1();

        new Benchmark(new Probe() {
            @Override
            public void runOnce() {
                original.run();
            }
        }, 1, 0).print("Original class : ");
        System.out.println(original.i);

        final ITestStackClass1 instance = (ITestStackClass1) clazz.newInstance();

        instance.run();
        //timeService.time = 10000;
        root.extract();
        accessor.onTimer();

        Json json = Json.object();
        accessor.dump(json, IProfilerMXBean.STATE_FLAG);
        System.out.println(json.toObject());

        Thread[] threads = new Thread[4];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Scope[] scopes = Tests.get(accessor.get().scopes, "activeScopes");
                        Scope scope1 = scopes[0];
                        List<IProbeCollector> collectors = Tests.get(scope1, "collectors");
                        StackProbeRootCollector root = (StackProbeRootCollector) collectors.get(0);

                        for (int i = 0; i < 100; i++) {
                            System.out.println("----------------------------------");
                            new Benchmark(new Probe() {
                                @Override
                                public void runOnce() {
                                    instance.run();
                                }
                            }, 1, 0).print("Instrumented pass " + i + ":");


                            if (i > 0 && i % 5 == 0) {
                                //timeService.time += 1000;
                                root.extract();
                            }

                            System.out.println(instance.get());
                            System.out.println(root.dump(IProfilerMXBean.STATE_FLAG));
                        }
                    } catch (Exception e) {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            });
            threads[k].start();
        }

        for (int k = 0; k < threads.length; k++)
            threads[k].join();

        accessor.get().scopes.extract();

        assertThat(measurementHandler.measurements.isEmpty(), is(false));
    }

    @Test
    public void testLZ4StackProbe() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(null, Arrays.asList(
                new ClassNameFilter(LZ4.class.getPackage().getName() + "*"),
                new ClassNameFilter(TestStackClass1.class.getPackage().getName() + "*")), null), false, null),
                new MemberFilter("*"));
        StackInterceptPointcut pointcut = new StackInterceptPointcut("test", filter, ThreadLocalAccessor.underAgent ?
                AgentStackProbeInterceptor.class : AgentlessStackProbeInterceptor.class);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(ClassLoader.getSystemClassLoader(), ClassLoader.getSystemClassLoader(),
                Arrays.asList(TestStackClass1.class.getName(), LZ4.class.getPackage().getName(), LZ4PerfTests.class.getName()), classTransformer);

        Class clazz = classLoader.loadClass(TestStackClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestStackClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

//        System.out.println("----------------------------------");
//        System.out.println(root.dump(IProfilerMXBean.STATE_FLAG));
//
//        System.out.println("----------------------------------");
//        final TestStackClass1 original = new TestStackClass1();
//
//        new Benchmark(new Probe()
//        {
//            @Override
//            public void runOnce()
//            {
//                original.testLZ4();        
//            }
//        }, 1, 0).print("Original class : ");
//        System.out.println(original.i);

        final ITestStackClass1 instance = (ITestStackClass1) clazz.newInstance();

        Timer timer = new Timer(1000, new ITimerListener() {
            //long time = System.currentTimeMillis();

            @Override
            public void onTimer() {
                accessor.onTimer();

//                long currentTime = System.currentTimeMillis();
//                if (strategy.allow() &&  currentTime > time + 120000)
//                {
//                    strategy.setAllowed(false);
//                    time = currentTime;
//                }
//                else if (!strategy.allow() &&  currentTime > time + 20000)
//                {
//                    strategy.setAllowed(true);
//                    time = currentTime;
//                }
//                count++;
//                
//                if ((count % 3) == 0)
//                {
//                    Json json = Json.object();
//                    accessor.dump(json, IProfilerMXBean.STATE_FLAG);
//                    System.out.println(json.toObject());
//                }
            }
        }, false, "timer", null);
        timer.start();
        Thread[] threads = new Thread[1];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        instance.testLZ4();
//                        //for (int i = 0; i < 100; i++)
//                        {
//                            System.out.println("----------------------------------");
//                            new Benchmark(new Probe()
//                            {
//                                @Override
//                                public void runOnce()
//                                {
//                                    instance.run();
//                                }
//                            }, 1, 0).print("Instrumented pass " + i + ":");
//                        }
                    } catch (Exception e) {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            });
            threads[k].start();
        }

        for (int k = 0; k < threads.length; k++)
            threads[k].join();

        timer.stop();
    }

    @Test
    public void testStackProbe2() throws Throwable {
        System.out.println("----------------------------------");
        //final TestStackClass3 original = new TestStackClass3();

        //final ExternalMeasurementStrategy strategy = measurementStrategyManager.findMeasurementStrategy("strategy1");
//        new Benchmark(new Probe()
//        {
//            @Override
//            public void runOnce()
//            {
//                original.run();        
//            }
//        }, 1, 0).print("Original class : ");
//        
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

//        Scope[] scopes = Tests.get(accessor.get().scopes, "activeScopes");
//        Scope scope1 = scopes[0];
//        List<IProbeCollector> collectors = Tests.get(scope1, "collectors");
//        StackProbeRootCollector root = (StackProbeRootCollector)collectors.get(0);
//        
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(TestStackClass3.class.getName() + "*"),
                new MemberFilter(Arrays.asList(new MemberFilter("*")), Arrays.asList(new MemberFilter(""/**delay*"*/)/*,
                new MemberFilter("*fast*"), new MemberFilter("*normal*")*/)));
        StackInterceptPointcut pointcut = new StackInterceptPointcut("test", filter, ThreadLocalAccessor.underAgent ?
                AgentStackProbeInterceptor.class : AgentlessStackProbeInterceptor.class);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestStackClass3.class.getName(), classTransformer);

        Class clazz = classLoader.loadClass(TestStackClass3.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestStackClass3.class.getName().replace('.', '/') + ".class").exists(), is(true));

        final Runnable instance = (Runnable) clazz.newInstance();

//        System.out.println("----------------------------------");
//        System.out.println(root.dump(IProfilerMXBean.STATE_FLAG));
//
//        instance.run();
//        timeService.time = 10000;
//        root.extract();
//        accessor.onTimer();

//        Json json = Json.object();
//        accessor.dump(json, IProfilerMXBean.STATE_FLAG);
//        System.out.println(json.toObject());
//        
        Timer timer = new Timer(1000, new ITimerListener() {
            //long time = System.currentTimeMillis();

            @Override
            public void onTimer() {
                accessor.onTimer();

//                long currentTime = System.currentTimeMillis();
//                if (strategy.allow() &&  currentTime > time + 120000)
//                {
//                    strategy.setAllowed(false);
//                    time = currentTime;
//                }
//                else if (!strategy.allow() &&  currentTime > time + 20000)
//                {
//                    strategy.setAllowed(true);
//                    time = currentTime;
//                }
//                count++;
//                
//                if ((count % 3) == 0)
//                {
//                    Json json = Json.object();
//                    accessor.dump(json, IProfilerMXBean.STATE_FLAG);
//                    System.out.println(json.toObject());
//                }
            }
        }, false, "timer", null);
        timer.start();
        Thread[] threads = new Thread[2];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        instance.run();
//                        //for (int i = 0; i < 100; i++)
//                        {
//                            System.out.println("----------------------------------");
//                            new Benchmark(new Probe()
//                            {
//                                @Override
//                                public void runOnce()
//                                {
//                                    instance.run();
//                                }
//                            }, 1, 0).print("Instrumented pass " + i + ":");
//                        }
                    } catch (Exception e) {
                        Exceptions.wrapAndThrow(e);
                    }
                }
            });
            threads[k].start();
        }

        for (int k = 0; k < threads.length; k++)
            threads[k].join();

        timer.stop();
    }

    @Test
    public void testStackProbe3() throws Throwable {
        AppStackCounterProvider provider = new AppStackCounterProvider(AppStackCounterType.ALLOCATION_BYTES, createProbeContext());
        long t = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
            provider.getValue();
        System.out.println(System.currentTimeMillis() - t);
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
                measurementHandler, measurementStrategyManager, new HashMap());

        return new ProbeContext(threadLocalAccessor, null, joinPointProvider, null, timeService, measurementHandler,
                configuration, new HashMap(), measurementStrategyManager);
    }
}
