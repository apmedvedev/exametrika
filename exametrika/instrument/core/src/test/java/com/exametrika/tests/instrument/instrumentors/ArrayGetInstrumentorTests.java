/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.ArrayGetInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestArrayGetClass;


/**
 * The {@link ArrayGetInstrumentorTests} are tests for {@link ArrayGetInstrumentor}.
 *
 * @author Medvedev-A
 * @see ArrayGetInstrumentor
 */
public class ArrayGetInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestArrayGetClass.class.getName() + "*"),
                false, null), null);
        ArrayGetPointcut pointcut = new ArrayGetPointcut("test", filter, new TestInterceptorConfiguration(), false, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestArrayGetClass.class.getName(), classTransformer);
        Class<TestArrayGetClass> clazz = (Class<TestArrayGetClass>) classLoader.loadClass(TestArrayGetClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestArrayGetClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        // Test primitive single dimension array
        InterceptorMock interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testSingle", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testSingle", null, null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testSingle").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test primitive multi dimension array
        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testMulti", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testMulti", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testMulti").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test object single dimension array
        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testObjectSingle", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testObjectSingle", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testObjectSingle").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test object multi dimension array
        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testObjectMulti", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testObjectMulti", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testObjectMulti").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);
    }

    @Test
    public void testInterceptWithParameters() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestArrayGetClass.class.getName() + "*"),
                false, null), null);
        ArrayGetPointcut pointcut = new ArrayGetPointcut("test", filter, new TestInterceptorConfiguration(), true, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestArrayGetClass.class.getName(), classTransformer);
        Class<TestArrayGetClass> clazz = (Class<TestArrayGetClass>) classLoader.loadClass(TestArrayGetClass.class.getName());

        // Test primitive single dimension array
        InterceptorMock interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testSingle", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testSingle", null, null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        Object array = clazz.getMethod("testSingle").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, array, null, 123L, null, 1);

        // Test primitive multi dimension array
        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testMulti", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testMulti", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        array = clazz.getMethod("testMulti").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, array, null, new Object[]{0, 123, 0, 0, 0, 0, 0, 0, 0, 0}, null, 1);

        // Test object single dimension array
        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testObjectSingle", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testObjectSingle", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        array = clazz.getMethod("testObjectSingle").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, array, null, "test", null, 1);

        interceptor = getInterceptor(pointcut, TestArrayGetClass.class.getName(), "testObjectMulti", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.ARRAY_GET, pointcut, TestArrayGetClass.class.getName(), "testObjectMulti", null, null);

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        array = clazz.getMethod("testObjectMulti").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, array, null, new Object[]{null, "test", null, null, null, null, null, null, null, null}, null, 1);
    }
}
