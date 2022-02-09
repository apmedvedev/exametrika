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
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.FieldGetInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestFieldGetClass;


/**
 * The {@link FieldGetInstrumentorTests} are tests for {@link FieldGetInstrumentor}.
 *
 * @author Medvedev-A
 * @see FieldGetInstrumentor
 */
public class FieldGetInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestFieldGetClass.class.getName() + "*"),
                false, null), null);
        FieldGetPointcut pointcut = new FieldGetPointcut("test", filter, new TestInterceptorConfiguration(), null, false, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestFieldGetClass.class.getName(), classTransformer);

        Class<TestFieldGetClass> clazz = (Class<TestFieldGetClass>) classLoader.loadClass(TestFieldGetClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestFieldGetClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        // Test primitive field
        InterceptorMock interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testPrimitive", interceptorManager);

        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testPrimitive", TestFieldGetClass.class.getName(), "f1");

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testPrimitive").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test primitive static field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testStaticPrimitive", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testStaticPrimitive", TestFieldGetClass.class.getName(), "f2");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testStaticPrimitive").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test object field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testObject", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testObject", TestFieldGetClass.class.getName(), "f3");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testObject").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        // Test object static field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testStaticObject", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testStaticObject", TestFieldGetClass.class.getName(), "f4");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        clazz.getMethod("testStaticObject").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);
    }

    @Test
    public void testInterceptWithParameters() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestFieldGetClass.class.getName() + "*"),
                false, null), null);
        FieldGetPointcut pointcut = new FieldGetPointcut("test", filter, new TestInterceptorConfiguration(), null, true, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestFieldGetClass.class.getName(), classTransformer);

        Class<TestFieldGetClass> clazz = (Class<TestFieldGetClass>) classLoader.loadClass(TestFieldGetClass.class.getName());

        // Test primitive field
        InterceptorMock interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testPrimitive", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testPrimitive", TestFieldGetClass.class.getName(), "f1");

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        Long value1 = (Long) clazz.getMethod("testPrimitive").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, value1, null, -1);

        // Test primitive static field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testStaticPrimitive", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testStaticPrimitive", TestFieldGetClass.class.getName(), "f2");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        Integer value2 = (Integer) clazz.getMethod("testStaticPrimitive").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, value2, null, -1);

        // Test object field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testObject", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testObject", TestFieldGetClass.class.getName(), "f3");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        String value3 = (String) clazz.getMethod("testObject").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, instance, null, value3, null, -1);

        // Test object static field
        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testStaticObject", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testStaticObject", TestFieldGetClass.class.getName(), "f4");

        assertThat(interceptor.intercept == null, is(true));

        instance = clazz.newInstance();
        String value4 = (String) clazz.getMethod("testStaticObject").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, value4, null, -1);
    }

    @Test
    public void testInterceptFilter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestFieldGetClass.class.getName() + "*"),
                false, null), null);
        FieldGetPointcut pointcut = new FieldGetPointcut("test", filter, new TestInterceptorConfiguration(),
                new QualifiedMemberNameFilter(null, new MemberNameFilter("f1")), false, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestFieldGetClass.class.getName(), classTransformer);

        Class<TestFieldGetClass> clazz = (Class<TestFieldGetClass>) classLoader.loadClass(TestFieldGetClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testPrimitive", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.FIELD_GET, pointcut, TestFieldGetClass.class.getName(), "testPrimitive", TestFieldGetClass.class.getName(), "f1");

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testPrimitive").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);

        interceptor = getInterceptor(pointcut, TestFieldGetClass.class.getName(), "testStaticPrimitive", interceptorManager);
        assertThat(interceptor, nullValue());
    }
}
