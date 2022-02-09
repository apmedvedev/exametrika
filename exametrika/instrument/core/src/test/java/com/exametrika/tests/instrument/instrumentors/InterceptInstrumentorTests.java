/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
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
import com.exametrika.impl.instrument.instrumentors.InterceptInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.ITestInterface1;
import com.exametrika.tests.instrument.instrumentors.data.TestInterceptClass1;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * The {@link InterceptInstrumentorTests} are tests for {@link InterceptInstrumentor}.
 *
 * @author Medvedev-A
 * @see InterceptInstrumentor
 */
public class InterceptInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testInterceptBefore() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(ITestInterface1.class.getName() + "*",
                Arrays.asList(new ClassNameFilter(TestInterceptClass1.InnerClass1.class.getName()),
                        new ClassNameFilter(TestInterceptClass1.InnerClass2.class.getName())), null),
                true, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestInterceptClass1.class.getName(), classTransformer);

        Class<TestInterceptClass1> clazz = (Class<TestInterceptClass1>) classLoader.loadClass(TestInterceptClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestInterceptClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object instance = clazz.getConstructor(int.class, String.class).newInstance(0, "test");

        assertThat((Integer) Tests.get(instance, "f2"), is(123));
        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "testStatic", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        clazz.getMethod("testStatic", int.class, String.class).invoke(null, 0, "test");

        assertThat((Integer) Tests.getStatic(clazz, "f1"), is(321));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, null, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test1", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test2", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test2", int.class).invoke(instance, 1);
        assertThat((String) o, is("return2"));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(instance, "f6"), is(0));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test public void test3()
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test3", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test3", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test3").invoke(instance);
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static inner class <init>
        Class<TestInterceptClass1.InnerClass1> innerClass1 = (Class<TestInterceptClass1.InnerClass1>) classLoader.loadClass(TestInterceptClass1.InnerClass1.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object inner = innerClass1.getConstructor(String.class).newInstance("test");

        assertThat((Integer) Tests.get(inner, "f1"), is(1234));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, inner, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = innerClass1.getMethod("test", String.class).invoke(inner, "test");
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(inner, "f2"), is(1234));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, inner, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test inner class <init>
        Class<TestInterceptClass1.InnerClass2> innerClass2 = (Class<TestInterceptClass1.InnerClass2>) classLoader.loadClass(TestInterceptClass1.InnerClass2.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        inner = innerClass2.getConstructor(clazz, String.class).newInstance(instance, "test");

        assertThat((Integer) Tests.get(inner, "f1"), is(1234));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, inner, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = innerClass2.getMethod("test", String.class).invoke(inner, "test");
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(inner, "f2"), is(1234));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, inner, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
    }

    @Test
    public void testInterceptAfter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestInterceptClass1.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.RETURN_EXIT),
                new TestInterceptorConfiguration(), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestInterceptClass1.class.getName(), classTransformer);
        Class<TestInterceptClass1> clazz = (Class<TestInterceptClass1>) classLoader.loadClass(TestInterceptClass1.class.getName());

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        Object instance = clazz.getConstructor(int.class, String.class).newInstance(0, "test");

        assertThat((Integer) Tests.get(instance, "f2"), is(123));
        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "testStatic", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        clazz.getMethod("testStatic", int.class, String.class).invoke(null, 0, "test");

        assertThat((Integer) Tests.getStatic(clazz, "f1"), is(321));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, null, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test1", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test2", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        o = clazz.getMethod("test2", int.class).invoke(instance, 1);
        assertThat((String) o, is("return2"));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(instance, "f6"), is(0));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test public void test3()
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test3", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test3", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        o = clazz.getMethod("test3").invoke(instance);
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static inner class <init>
        Class<TestInterceptClass1.InnerClass1> innerClass1 = (Class<TestInterceptClass1.InnerClass1>) classLoader.loadClass(TestInterceptClass1.InnerClass1.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        Object inner = innerClass1.getConstructor(String.class).newInstance("test");

        assertThat((Integer) Tests.get(inner, "f1"), is(1234));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, inner, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test static inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        o = innerClass1.getMethod("test", String.class).invoke(inner, "test");
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(inner, "f2"), is(1234));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, inner, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test inner class <init>
        Class<TestInterceptClass1.InnerClass2> innerClass2 = (Class<TestInterceptClass1.InnerClass2>) classLoader.loadClass(TestInterceptClass1.InnerClass2.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        inner = innerClass2.getConstructor(clazz, String.class).newInstance(instance, "test");

        assertThat((Integer) Tests.get(inner, "f1"), is(1234));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, inner, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        // Test inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        o = innerClass2.getMethod("test", String.class).invoke(inner, "test");
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(inner, "f2"), is(1234));

        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, inner, null, null, null, null, -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
    }

    @Test
    public void testInterceptAfterException() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestInterceptClass1.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.THROW_EXIT),
                new TestInterceptorConfiguration(), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestInterceptClass1.class.getName(), classTransformer);

        final Class<TestInterceptClass1> clazz = (Class<TestInterceptClass1>) classLoader.loadClass(TestInterceptClass1.class.getName());

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getConstructor(int.class, String.class).newInstance(0, null);
            }
        });

        assertThat(interceptor.throwExit.getThis() != null, is(true));
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "testStatic", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        Expected expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testStatic", int.class, String.class).invoke(null, 0, null);
            }
        });

        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, null, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test1", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        final Object instance = clazz.getConstructor(int.class, String.class).newInstance(0, "test");
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, null);
            }
        });

        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test2", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RuntimeException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test2", int.class).invoke(instance, 0);
            }
        });
        assertThat((Integer) Tests.get(instance, "f4"), is(0));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(instance, "f6"), is(123));

        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test static inner class <init>
        final Class<TestInterceptClass1.InnerClass1> innerClass1 = (Class<TestInterceptClass1.InnerClass1>) classLoader.loadClass(TestInterceptClass1.InnerClass1.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                innerClass1.getConstructor(String.class).newInstance((String) null);
            }
        });
        assertThat(interceptor.throwExit.getThis() != null, is(true));
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test static inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass1.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        final Object inner = innerClass1.getConstructor(String.class).newInstance("test");
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                innerClass1.getMethod("test", String.class).invoke(inner, (String) null);
            }
        });

        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, inner, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test inner class <init>
        final Class<TestInterceptClass1.InnerClass2> innerClass2 = (Class<TestInterceptClass1.InnerClass2>) classLoader.loadClass(TestInterceptClass1.InnerClass2.class.getName());
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                innerClass2.getConstructor(clazz, String.class).newInstance(instance, (String) null);
            }
        });

        assertThat(interceptor.throwExit.getThis() != null, is(true));
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test inner class public void test(String p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.InnerClass2.class.getName(), "test", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));

        final Object inner2 = innerClass2.getConstructor(clazz, String.class).newInstance(instance, "test");
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                innerClass2.getMethod("test", String.class).invoke(inner2, (String) null);
            }
        });

        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, inner2, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
    }

    @Test
    public void testInterceptAroundWithParameters() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestInterceptClass1.class.getName() + "*"),
                false, null), null);
        InterceptPointcut pointcut = new InterceptPointcut("test", filter, Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT,
                InterceptPointcut.Kind.THROW_EXIT), new TestInterceptorConfiguration(), true, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestInterceptClass1.class.getName(), classTransformer);

        final Class<TestInterceptClass1> clazz = (Class<TestInterceptClass1>) classLoader.loadClass(TestInterceptClass1.class.getName());

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "<init>", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "<init>", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object instance = clazz.getConstructor(int.class, String.class).newInstance(0, "test");

        assertThat((Integer) Tests.get(instance, "f2"), is(123));
        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
        interceptor.returnExit = null;

        // Test <init> exception
        Expected expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getConstructor(int.class, String.class).newInstance(0, null);
            }
        });

        assertThat(interceptor.enter.getThis() != null, is(true));
        assertThat(Arrays.equals(interceptor.enter.getParams(), new Object[]{0, null}), is(true));
        assertThat(interceptor.throwExit.getThis() != null, is(true));
        assertThat(interceptor.throwExit.getException() == expected.getException().getCause(), is(true));
        assertThat(interceptor.returnExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "testStatic", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        clazz.getMethod("testStatic", int.class, String.class).invoke(null, 0, "test");

        assertThat((Integer) Tests.getStatic(clazz, "f1"), is(321));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, null, null, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, null, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
        interceptor.returnExit = null;

        // Test static exception
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testStatic", int.class, String.class).invoke(null, 0, null);
            }
        });

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, null, null, new Object[]{0, null}, null, null, -1);
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, null, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.returnExit == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test1", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, 123L, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
        interceptor.returnExit = null;

        // Test public long test1(int p1, String p2) with exception
        final Object instance2 = clazz.getConstructor(int.class, String.class).newInstance(0, "test");
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test1", int.class, String.class).invoke(instance2, 0, null);
            }
        });

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance2, null, new Object[]{0, null}, null, null, -1);
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance2, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.returnExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test2", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test2", int.class).invoke(instance, 1);
        assertThat((String) o, is("return2"));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(instance, "f6"), is(0));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{1}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, "return2", null, -1);
        assertThat(interceptor.throwExit == null, is(true));
        interceptor.returnExit = null;

        // Test public String test2(int p1) with exception
        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RuntimeException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test2", int.class).invoke(instance2, 0);
            }
        });

        assertThat((Integer) Tests.get(instance2, "f4"), is(0));
        assertThat((Integer) Tests.get(instance2, "f5"), is(123));
        assertThat((Integer) Tests.get(instance2, "f6"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance2, null, new Object[]{0}, null, null, -1);
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance2, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.returnExit == null, is(true));

        // Test public void test3()
        interceptor = getInterceptor(pointcut, TestInterceptClass1.class.getName(), "test3", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, TestInterceptClass1.class.getName(), "test3", null, null);

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test3").invoke(instance);
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(instance, "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
    }
}
