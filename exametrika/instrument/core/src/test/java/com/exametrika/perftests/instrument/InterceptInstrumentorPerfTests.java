/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.instrument;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.spi.instrument.boot.StaticInterceptor;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;
import com.exametrika.tests.instrument.instrumentors.TestClassLoader;


/**
 * The {@link InterceptInstrumentorPerfTests} are performance tests for {@link InterceptInstrumentor}.
 *
 * @author Medvedev-A
 * @see InterceptInstrumentor
 */
public class InterceptInstrumentorPerfTests {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(InterceptInstrumentorPerfTests.class);

    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestClass.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT,
                InterceptPointcut.Kind.THROW_EXIT), new TestInterceptorConfiguration(), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestClass.class.getName(), classTransformer);
        Class<TestClass> clazz = (Class<TestClass>) classLoader.loadClass(TestClass.class.getName());
        final ITestInterface instance = clazz.newInstance();
        InterceptorMock interceptor = getInterceptor(pointcut, TestClass.class.getName(), "test", interceptorManager);

        logger.log(LogLevel.INFO, messages.intercept(new Benchmark(new Probe() {
            @Override
            public long run() {
                for (int i = 0; i < 100000000; i++)
                    instance.test(1, 2);

                return 100000000;
            }
        }, 1)));

        Assert.assertTrue(interceptor.intercept);
    }

    @Test
    public void testInterceptWithParameters() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestClass.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT,
                InterceptPointcut.Kind.THROW_EXIT), new TestInterceptorConfiguration(), true, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestClass.class.getName(), classTransformer);
        Class<TestClass> clazz = (Class<TestClass>) classLoader.loadClass(TestClass.class.getName());
        final ITestInterface instance = clazz.newInstance();
        InterceptorMock interceptor = getInterceptor(pointcut, TestClass.class.getName(), "test", interceptorManager);

        logger.log(LogLevel.INFO, messages.interceptWithParameters(new Benchmark(new Probe() {
            @Override
            public long run() {
                for (int i = 0; i < 100000000; i++)
                    instance.test(1, 2);
                return 100000000;
            }
        }, 1)));

        Assert.assertTrue(interceptor.intercept);
    }

    @Test
    public void testStaticIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestClass.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT,
                InterceptPointcut.Kind.THROW_EXIT), new TestStaticInterceptorConfiguration(), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(new InterceptorManager(), getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestClass.class.getName(), classTransformer);
        Class<TestClass> clazz = (Class<TestClass>) classLoader.loadClass(TestClass.class.getName());
        final ITestInterface instance = clazz.newInstance();

        logger.log(LogLevel.INFO, messages.customIntercept(new Benchmark(new Probe() {
            @Override
            public void afterWarmUp() {
                TestStaticInterceptor.enterCount = 0;
                TestStaticInterceptor.returnExitCount = 0;
            }

            @Override
            public long run() {
                for (int i = 0; i < 1000000000; i++)
                    instance.test(1, 2);

                return 1000000000;

            }
        }, 1)));

        Assert.assertThat(TestStaticInterceptor.enterCount, is(1000000000L));
        Assert.assertThat(TestStaticInterceptor.returnExitCount, is(1000000000L));
    }

    private InterceptorMock getInterceptor(Pointcut pointcut, String className, String methodName, IInterceptorManager interceptorManager) throws Exception {
        Object[] list = (Object[]) (Tests.get(Tests.get(interceptorManager, "entries"), "elements"));
        for (Object o : list) {
            InterceptorMock interceptor = Tests.get(o, "interceptor");
            if (interceptor.joinPoint.getPointcut().equals(pointcut) &&
                    interceptor.joinPoint.getClassName().equals(className) &&
                    interceptor.joinPoint.getMethodName().equals(methodName))
                return interceptor;
        }

        return null;
    }

    public interface ITestInterface {
        int test(int param1, int param2);
    }

    public static class InterceptorMock implements IDynamicInterceptor {
        public boolean intercept;
        public IJoinPoint joinPoint;

        @Override
        public boolean intercept(IInvocation invocation) {
            this.intercept = true;
            return true;
        }

        @Override
        public void start(IJoinPoint joinPoint) {
            this.joinPoint = joinPoint;
        }

        @Override
        public void stop(boolean close) {
            this.intercept = false;
        }
    }

    public static class TestClass implements ITestInterface {
        @Override
        public int test(int param1, int param2) {
            return 0;
        }
    }

    public static class TestStaticInterceptor extends StaticInterceptor {
        private static long enterCount;
        private static long returnExitCount;

        public static Object onEnter(int index, int version, Object instance, Object[] params) {
            enterCount++;
            return null;
        }

        public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
            returnExitCount++;
        }

        public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        }
    }

    private static class TestInterceptorConfiguration extends DynamicInterceptorConfiguration {
        @Override
        public IDynamicInterceptor createInterceptor() {
            return new InterceptorMock();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestInterceptorConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    private static class TestStaticInterceptorConfiguration extends StaticInterceptorConfiguration {
        public TestStaticInterceptorConfiguration() {
            super(TestStaticInterceptor.class);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestStaticInterceptorConfiguration))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    private interface IMessages {
        @DefaultMessage("Intercept without parameters: {0}.")
        ILocalizedMessage intercept(Object results);

        @DefaultMessage("Intercept with parameters: {0}.")
        ILocalizedMessage interceptWithParameters(Object results);

        @DefaultMessage("Custom intercept: {0}.")
        ILocalizedMessage customIntercept(Object results);
    }
}
