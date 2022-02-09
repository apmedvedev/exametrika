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
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.ITestable;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.ICondition;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.CallInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestCalleeClass;
import com.exametrika.tests.instrument.instrumentors.data.TestCallerClass;


/**
 * The {@link CallInstrumentorTests} are tests for {@link CallInstrumentor}.
 *
 * @author Medvedev-A
 * @see CallInstrumentor
 */
public class CallInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestCallerClass.class.getName() + "*"),
                false, null), null);
        CallPointcut pointcut = new CallPointcut("test", filter, new TestInterceptorConfiguration(), null, false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestCallerClass.class.getName(), classTransformer);

        Class<TestCallerClass> clazz = (Class<TestCallerClass>) classLoader.loadClass(TestCallerClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestCallerClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testInit", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testInit", TestCalleeClass.class.getName(), "<init>");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object instance = clazz.getConstructor().newInstance();
        clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, "test");

        assertThat((Integer) Tests.get(instance, "f1"), is(123));
        assertThat((Integer) Tests.get(instance, "f2"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testStatic", TestCalleeClass.class.getName(), "testStatic");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        clazz.getMethod("testStatic", int.class, String.class).invoke(instance, 0, "test");

        assertThat((Integer) Tests.get(instance, "f3"), is(123));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f1"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test1", TestCalleeClass.class.getName(), "test1");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test2", TestCalleeClass.class.getName(), "test2");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test2", long.class).invoke(instance, 1);
        assertThat((String) o, is("return2"));
        assertThat((Integer) Tests.get(instance, "f6"), is(123));
        assertThat((Integer) Tests.get(instance, "f8"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f4"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f5"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public void test3()
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test3", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test3", TestCalleeClass.class.getName(), "test3");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test3").invoke(instance);
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(instance, "f9"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f7"), is(123));


        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
    }

    @Test
    public void testInterceptWithParameters() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestCallerClass.class.getName() + "*"),
                false, null), null);
        CallPointcut pointcut = new CallPointcut("test", filter, new TestInterceptorConfiguration(), null, true, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestCallerClass.class.getName(), classTransformer);

        final Class<TestCallerClass> clazz = (Class<TestCallerClass>) classLoader.loadClass(TestCallerClass.class.getName());

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testInit", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testInit", TestCalleeClass.class.getName(), "<init>");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        final Object instance = clazz.getConstructor().newInstance();

        Expected expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, null);
            }
        });

        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, null}, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
        interceptor.throwExit = null;

        clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, "test");

        Object callee = Tests.get(instance, "callee");

        assertThat((Integer) Tests.get(instance, "f1"), is(123));
        assertThat((Integer) Tests.get(instance, "f2"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, callee, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test static
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testStatic", TestCalleeClass.class.getName(), "testStatic");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testStatic", int.class, String.class).invoke(instance, 0, null);
            }
        });

        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, null}, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
        interceptor.throwExit = null;

        clazz.getMethod("testStatic", int.class, String.class).invoke(instance, 0, "test");

        assertThat((Integer) Tests.get(instance, "f3"), is(123));
        assertThat((Integer) Tests.get(instance, "f4"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f1"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test1", TestCalleeClass.class.getName(), "test1");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

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

        assertThat((Integer) Tests.get(instance, "f5"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, callee, new Object[]{0, null}, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, callee, null, null, expected.getException().getCause(), -1);
        interceptor.throwExit = null;

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, callee, new Object[]{0, "test"}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, callee, null, 123L, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test2", TestCalleeClass.class.getName(), "test2");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RuntimeException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test2", long.class).invoke(instance, 0);
            }
        });

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, callee, new Object[]{0L}, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, callee, null, null, expected.getException().getCause(), -1);
        interceptor.throwExit = null;

        o = clazz.getMethod("test2", long.class).invoke(instance, 1);
        assertThat((String) o, is("return2"));
        assertThat((Integer) Tests.get(instance, "f6"), is(123));
        assertThat((Integer) Tests.get(instance, "f8"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f4"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f5"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, callee, new Object[]{1L}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, callee, null, "return2", null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        // Test public void test3()
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test3", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test3", TestCalleeClass.class.getName(), "test3");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        o = clazz.getMethod("test3").invoke(instance);
        assertThat(o, nullValue());
        assertThat((Integer) Tests.get(instance, "f9"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f7"), is(123));


        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, callee, new Object[]{}, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, callee, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));
    }

    @Test
    public void testInterceptWithException() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestCallerClass.class.getName() + "*"),
                false, null), null);
        CallPointcut pointcut = new CallPointcut("test", filter, new TestInterceptorConfiguration(), null, false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestCallerClass.class.getName(), classTransformer);

        final Class<TestCallerClass> clazz = (Class<TestCallerClass>) classLoader.loadClass(TestCallerClass.class.getName());

        // Test <init>
        InterceptorMock interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testInit", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testInit", TestCalleeClass.class.getName(), "<init>");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        final Object instance = clazz.getConstructor().newInstance();

        Expected expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, null);
            }
        });

        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
        assertThat(interceptor.returnExit == null, is(true));

        clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, "test");

        // Test static
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "testStatic", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "testStatic", TestCalleeClass.class.getName(), "testStatic");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause() instanceof IllegalArgumentException;
            }
        }, IllegalArgumentException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("testStatic", int.class, String.class).invoke(instance, 0, null);
            }
        });

        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);

        // Test public long test1(int p1, String p2)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test1", TestCalleeClass.class.getName(), "test1");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

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

        assertThat((Integer) Tests.get(instance, "f5"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);

        // Test public String test2(int p1)
        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test2", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test2", TestCalleeClass.class.getName(), "test2");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        expected = new Expected(new ICondition<Throwable>() {
            @Override
            public boolean evaluate(Throwable value) {
                return value.getCause().getClass() == RuntimeException.class;
            }
        }, RuntimeException.class, new ITestable() {
            @Override
            public void test() throws Throwable {
                clazz.getMethod("test2", long.class).invoke(instance, 0);
            }
        });

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        assertThat(interceptor.returnExit == null, is(true));
        checkInvocation(interceptor.throwExit, IInvocation.Kind.THROW_EXIT, instance, null, null, null, expected.getException().getCause(), -1);
    }

    @Test
    public void testInterceptFilter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestCallerClass.class.getName() + "*"),
                false, null), null);
        CallPointcut pointcut = new CallPointcut("test", filter, new TestInterceptorConfiguration(),
                new QualifiedMemberNameFilter(null, new MemberNameFilter("test1(*")), false, false, 0);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestCallerClass.class.getName(), classTransformer);

        final Class<TestCallerClass> clazz = (Class<TestCallerClass>) classLoader.loadClass(TestCallerClass.class.getName());

        Object instance = clazz.getConstructor().newInstance();
        clazz.getMethod("testInit", int.class, String.class).invoke(instance, 0, "test");

        InterceptorMock interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test1", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CALL, pointcut, TestCallerClass.class.getName(), "test1", TestCalleeClass.class.getName(), "test1");

        assertThat(interceptor.enter == null, is(true));
        assertThat(interceptor.returnExit == null, is(true));
        assertThat(interceptor.throwExit == null, is(true));

        Object o = clazz.getMethod("test1", int.class, String.class).invoke(instance, 0, "test");
        assertThat((Long) o, is(123L));
        assertThat((Integer) Tests.get(instance, "f5"), is(123));
        assertThat((Integer) Tests.get(Tests.get(instance, "callee"), "f4"), is(123));

        checkInvocation(interceptor.enter, IInvocation.Kind.ENTER, instance, null, null, null, null, -1);
        checkInvocation(interceptor.returnExit, IInvocation.Kind.RETURN_EXIT, instance, null, null, null, null, -1);
        assertThat(interceptor.throwExit == null, is(true));

        interceptor = getInterceptor(pointcut, TestCallerClass.class.getName(), "test2", interceptorManager);
        assertThat(interceptor, nullValue());
    }
}
