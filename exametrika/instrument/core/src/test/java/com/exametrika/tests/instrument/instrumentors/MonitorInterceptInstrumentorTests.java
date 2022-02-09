/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.ITestable;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.ICondition;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.MonitorInterceptInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestMonitorClass;


/**
 * The {@link MonitorInterceptInstrumentorTests} are tests for {@link MonitorInterceptInstrumentor}.
 *
 * @author Medvedev-A
 * @see MonitorInterceptInstrumentor
 */
public class MonitorInterceptInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testInterceptBeforeEnter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.BEFORE_ENTER),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);

        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestMonitorClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_ENTER, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();
        clazz.getMethod("test4", boolean.class).invoke(instance, false);
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptAfterEnter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.AFTER_ENTER),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_ENTER, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();
        clazz.getMethod("test4", boolean.class).invoke(instance, false);
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptBeforeExit() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.BEFORE_EXIT),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();
        clazz.getMethod("test4", boolean.class).invoke(instance, false);
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptAfterExit() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.AFTER_EXIT),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();
        clazz.getMethod("test4", boolean.class).invoke(instance, false);
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptBeforeExitException() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.BEFORE_EXIT),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 1, interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test4", boolean.class).invoke(instance, true);
            }
        });

        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptAfterExitException() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.AFTER_EXIT),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 1, interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor.intercept == null, is(true));

        final Object instance = clazz.newInstance();

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test4", boolean.class).invoke(instance, true);
            }
        });

        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
    }

    @Test
    public void testInterceptAll() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestMonitorClass.class.getName() + "*"),
                false, null), null);
        MonitorInterceptPointcut pointcut = new MonitorInterceptPointcut("test", filter, Enums.of(MonitorInterceptPointcut.Kind.BEFORE_ENTER,
                MonitorInterceptPointcut.Kind.AFTER_ENTER, MonitorInterceptPointcut.Kind.BEFORE_EXIT, MonitorInterceptPointcut.Kind.AFTER_EXIT),
                new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestMonitorClass.class.getName(), classTransformer);
        final Class<TestMonitorClass> clazz = (Class<TestMonitorClass>) classLoader.loadClass(TestMonitorClass.class.getName());

        InterceptorMock interceptor1 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 0, interceptorManager);
        checkJoinPoint(interceptor1.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_ENTER, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor1.intercept == null, is(true));

        InterceptorMock interceptor2 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 1, interceptorManager);
        checkJoinPoint(interceptor2.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_ENTER, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor2.intercept == null, is(true));

        InterceptorMock interceptor3 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 2, interceptorManager);
        checkJoinPoint(interceptor3.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor3.intercept == null, is(true));

        InterceptorMock interceptor4 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 3, interceptorManager);
        checkJoinPoint(interceptor4.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor4.intercept == null, is(true));

        InterceptorMock interceptor5 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 4, interceptorManager);
        checkJoinPoint(interceptor5.joinPoint, IJoinPoint.Kind.BEFORE_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor5.intercept == null, is(true));

        InterceptorMock interceptor6 = getInterceptor(pointcut, TestMonitorClass.class.getName(), "test4", 5, interceptorManager);
        checkJoinPoint(interceptor6.joinPoint, IJoinPoint.Kind.AFTER_MONITOR_EXIT, pointcut, TestMonitorClass.class.getName(), "test4", null, null);
        assertThat(interceptor6.intercept == null, is(true));

        final Object instance = clazz.newInstance();
        clazz.getMethod("test4", boolean.class).invoke(instance, false);
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor1.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor2.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor3.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor4.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);

        assertThat(interceptor5.intercept, nullValue());
        assertThat(interceptor6.intercept, nullValue());
        interceptor1.intercept = null;
        interceptor2.intercept = null;
        interceptor3.intercept = null;
        interceptor4.intercept = null;

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test4", boolean.class).invoke(instance, true);
            }
        });

        checkInvocation(interceptor1.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor2.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor5.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);
        checkInvocation(interceptor6.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, null, null, -1);

        assertThat(interceptor3.intercept, nullValue());
        assertThat(interceptor4.intercept, nullValue());
    }
}
