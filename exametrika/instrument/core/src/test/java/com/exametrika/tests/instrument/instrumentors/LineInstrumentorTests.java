/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.LineInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;
import com.exametrika.tests.instrument.instrumentors.data.TestThrowClass;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * The {@link LineInstrumentorTests} are tests for {@link LineInstrumentor}.
 *
 * @author Medvedev-A
 * @see LineInstrumentor
 */
public class LineInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testInterceptBefore() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestThrowClass.class.getName() + "*"),
                false, null), null);
        LinePointcut pointcut = new LinePointcut("test", filter, new TestInterceptorConfiguration2(), 8, 8, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestThrowClass.class.getName(), classTransformer);

        Class<TestThrowClass> clazz = (Class<TestThrowClass>) classLoader.loadClass(TestThrowClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestThrowClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestThrowClass.class.getName(), "testThrow", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.LINE, pointcut, TestThrowClass.class.getName(), "testThrow", null, null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testThrow").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);
    }

    @Test
    public void testInterceptAfter() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestThrowClass.class.getName() + "*"),
                false, null), null);
        LinePointcut pointcut = new LinePointcut("test", filter, new TestInterceptorConfiguration3(), 11, 11, false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestThrowClass.class.getName(), classTransformer);

        Class<TestThrowClass> clazz = (Class<TestThrowClass>) classLoader.loadClass(TestThrowClass.class.getName());

        InterceptorMock interceptor = getInterceptor(pointcut, TestThrowClass.class.getName(), "testThrow", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.LINE, pointcut, TestThrowClass.class.getName(), "testThrow", null, null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testThrow").invoke(instance);

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, null, -1);
    }

    public static class InterceptorMock2 extends InterceptorMock {
        @Override
        public boolean intercept(IInvocation invocation) {
            boolean res = super.intercept(invocation);

            try {
                assertThat((Integer) Tests.get(invocation.getThis(), "f1"), is(0));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return res;
        }
    }

    public static class InterceptorMock3 extends InterceptorMock {
        @Override
        public boolean intercept(IInvocation invocation) {
            boolean res = super.intercept(invocation);
            try {
                assertThat((Integer) Tests.get(invocation.getThis(), "f1"), is(123));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return res;
        }
    }

    private static class TestInterceptorConfiguration2 extends DynamicInterceptorConfiguration {
        @Override
        public IDynamicInterceptor createInterceptor() {
            return new InterceptorMock2();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestInterceptorConfiguration2))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    private static class TestInterceptorConfiguration3 extends DynamicInterceptorConfiguration {
        @Override
        public IDynamicInterceptor createInterceptor() {
            return new InterceptorMock3();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestInterceptorConfiguration3))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
