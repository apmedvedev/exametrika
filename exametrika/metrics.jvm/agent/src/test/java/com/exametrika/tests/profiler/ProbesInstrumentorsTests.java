/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.instrument.config.InterceptPointcut.Kind;
import com.exametrika.api.metrics.jvm.config.HttpInterceptPointcut;
import com.exametrika.api.profiler.config.ThreadExitPointInterceptPointcut;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.tests.instrument.instrumentors.TestClassLoader;
import com.exametrika.tests.profiler.support.ITestClass1;
import com.exametrika.tests.profiler.support.TestClass1;
import com.exametrika.tests.profiler.support.TestProbeInterceptor;
import com.exametrika.tests.profiler.support.TestProbeInterceptor.TestInputStream;
import com.exametrika.tests.profiler.support.TestProbeInterceptor.TestRunnable;

/**
 * The {@link ProbesInstrumentorsTests} are tests for various probe instrumentors class.
 *
 * @author Medvedev-A
 */
public class ProbesInstrumentorsTests {
    @Test
    public void testThreadInterceptInstrumentor() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(TestClass1.class.getName() + "*"),
                new MemberFilter("execute(Runnable)*"));
        ThreadExitPointInterceptPointcut pointcut = new ThreadExitPointInterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER),
                new StaticInterceptorConfiguration(TestProbeInterceptor.class), true, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestClass1.class.getName(), classTransformer);

        Class<TestClass1> clazz = (Class<TestClass1>) classLoader.loadClass(TestClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        ITestClass1 instance = clazz.newInstance();

        TestRunnable1 runnable1 = new TestRunnable1();
        instance.execute(runnable1);

        TestRunnable runnable = Tests.get(instance, "runnable");
        assertThat(runnable.runnable == runnable1, is(true));
    }

    @Test
    public void testHttpConnectionInterceptInstrumentor() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(TestClass1.class.getName() + "*"),
                new MemberFilter("execute(Runnable)*"));
        HttpInterceptPointcut pointcut = new HttpInterceptPointcut("test", filter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                new StaticInterceptorConfiguration(TestProbeInterceptor.class), "java.io.InputStream onReturnExit(java.lang.Object,java.lang.Object)", 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestClass1.class.getName(), classTransformer);

        Class<TestClass1> clazz = (Class<TestClass1>) classLoader.loadClass(TestClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        ITestClass1 instance = clazz.newInstance();

        TestRunnable1 runnable1 = new TestRunnable1();
        TestInputStream runnable = (TestInputStream) instance.execute(runnable1);

        assertThat(runnable.runnable == runnable1, is(true));
    }

    private static class TestRunnable1 implements Runnable {
        @Override
        public void run() {
        }
    }
}
