/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Invocation;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;
import com.exametrika.tests.instrument.instrumentors.data.TestArrayGetClass;


/**
 * The {@link InterceptorManagerTests} are tests for {@link InterceptorManager}.
 *
 * @author Medvedev-A
 * @see InterceptorManager
 */
public class InterceptorManagerTests extends AbstractInstrumentorTests {
    @After
    public void tearDown() {
        InterceptorMock.exception = false;
        InterceptorMock.instance = null;
    }

    @Test
    public void testManager() {
        InterceptorManager manager = new InterceptorManager(false);
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), true, false, 0);
        IJoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "Test", "test", "test", 0, pointcut, null, null, null, null, null, 0);

        IInterceptorManager.JoinPointInfo info = manager.allocate(null, joinPoint);
        assertThat(info.index, is(0));
        assertThat(info.version, is(1));
        assertThat(InterceptorMock.instance != null, is(true));
        assertThat(InterceptorMock.instance.joinPoint == joinPoint, is(true));
        assertThat(InterceptorMock.instance.invocation, nullValue());

        IInvocation invocation = new Invocation();
        manager.invoke(0, 1, invocation);

        assertThat(InterceptorMock.instance != null, is(true));
        assertThat(InterceptorMock.instance.invocation == invocation, is(true));
        assertThat(InterceptorMock.instance.joinPoint == joinPoint, is(true));

        InterceptorMock instance = InterceptorMock.instance;
        manager.free(null, joinPoint.getClassName());
        assertThat(InterceptorMock.instance, nullValue());
        assertThat(instance.invocation, nullValue());

        info = manager.allocate(null, joinPoint);
        assertThat(info.index, is(0));
        assertThat(info.version, is(3));
        assertThat(InterceptorMock.instance != null, is(true));
        assertThat(InterceptorMock.instance.invocation, nullValue());
        assertThat(InterceptorMock.instance.joinPoint == joinPoint, is(true));

        manager.invoke(0, 1, invocation);
        assertThat(InterceptorMock.instance.invocation, nullValue());

        manager.invoke(10, 1, invocation);
        assertThat(InterceptorMock.instance.invocation, nullValue());

        manager.invoke(0, 3, invocation);
        assertThat(InterceptorMock.instance != null, is(true));
        assertThat(InterceptorMock.instance.invocation == invocation, is(true));
        assertThat(InterceptorMock.instance.joinPoint == joinPoint, is(true));

        instance = InterceptorMock.instance;

        manager.freeAll();
        assertThat(InterceptorMock.instance, nullValue());
        assertThat(instance.invocation, nullValue());

        info = manager.allocate(null, joinPoint);
        assertThat(info.index, is(0));
        assertThat(info.version, is(1));

        manager.invoke(0, 1, invocation);
        assertThat(InterceptorMock.instance != null, is(true));
        assertThat(InterceptorMock.instance.invocation == invocation, is(true));
        assertThat(InterceptorMock.instance.joinPoint == joinPoint, is(true));

        info = manager.allocate(null, joinPoint);

        InterceptorMock.exception = true;
        assertThat(manager.invoke(info.index, info.version, invocation), is(false));
    }

    @Test
    public void testExpunge() throws Exception {
        InterceptorManager manager = new InterceptorManager();
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null), Enums.of(InterceptPointcut.Kind.ENTER),
                new com.exametrika.tests.instrument.instrumentors.TestInterceptorConfiguration(), true, false, 0);
        IJoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "class", "method", "method", 0, pointcut, null, null, null, null, null, 0);

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        StaticClassTransformer classTransformer = new StaticClassTransformer(manager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));

        TestClassLoader classLoader = new TestClassLoader(TestArrayGetClass.class.getName(), classTransformer);

        IInterceptorManager.JoinPointInfo info = manager.allocate(classLoader, joinPoint);
        assertThat(info.index, is(0));
        assertThat(info.version, is(1));
        Invocation invocation = new Invocation();
        invocation.kind = IInvocation.Kind.INTERCEPT;
        manager.invoke(0, 1, invocation);

        com.exametrika.tests.instrument.instrumentors.InterceptorMock interceptor = getInterceptor(pointcut, "class", "method", manager);
        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, null, null, null, null, null, -1);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.INTERCEPT, pointcut, "class", "method", null, null);

        classLoader = null;

        int count = 2;
        while (count > 0) {
            System.gc();
            Thread.sleep(100);
            count--;
        }

        IJoinPoint joinPoint2 = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "class2", "method2", "method2", 0, pointcut, null, null, null, null, null, 0);
        manager.allocate(null, joinPoint2);

        assertThat(interceptor.intercept, nullValue());
        interceptor = getInterceptor(pointcut, "class", "method", manager);
        assertThat(interceptor, nullValue());
    }

    @Test
    public void testUpdateConfiguration() {
        InterceptorManager manager = new InterceptorManager(false);
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), true, false, 0);
        IJoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "Test", "test", "test", 0, pointcut, null, null, null, null, null, 0);

        manager.allocate(null, joinPoint);

        IInvocation invocation = new Invocation();
        manager.invoke(0, 1, invocation);

        InterceptorMock interceptor = InterceptorMock.instance;
        assertThat(interceptor.joinPoint == joinPoint, is(true));

        InterceptorConfiguration interceptorConfig = new TestInterceptorConfiguration();
        InterceptPointcut pointcut2 = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null), Enums.of(InterceptPointcut.Kind.ENTER),
                interceptorConfig, true, false, 0);
        IJoinPoint joinPoint2 = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "Test", "test", "test", 0, pointcut2, null, null, null, null, null, 0);

        manager.invoke(0, 1, invocation);

        assertThat(InterceptorMock.instance == interceptor, is(true));
        assertThat(interceptor.joinPoint, is(joinPoint2));
        assertThat(interceptor.stopped, is(false));
    }

    @Test
    public void testLazyInterceptorStart() {
        InterceptorManager manager = new InterceptorManager(true);
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), true, false, 0);
        IJoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "Test", "test", "test", 0, pointcut, null, null, null, null, null, 0);

        manager.allocate(null, joinPoint);

        assertThat(InterceptorMock.instance, nullValue());

        IInvocation invocation = new Invocation();
        manager.invoke(0, 1, invocation);

        assertThat(InterceptorMock.instance != null, is(true));
        InterceptorMock interceptor = InterceptorMock.instance;
        assertThat(interceptor.joinPoint == joinPoint, is(true));
    }

    public static class InterceptorMock implements IDynamicInterceptor {
        public static boolean exception;
        public static InterceptorMock instance;
        private IInvocation invocation;
        private IJoinPoint joinPoint;
        private boolean stopped;

        @Override
        public boolean intercept(IInvocation invocation) {
            if (exception)
                throw new RuntimeException("test exception");
            this.invocation = invocation;

            return true;
        }

        @Override
        public void start(IJoinPoint joinPoint) {
            instance = this;
            this.joinPoint = joinPoint;
        }

        @Override
        public void stop(boolean close) {
            this.invocation = null;
            instance = null;
            stopped = true;
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
}
