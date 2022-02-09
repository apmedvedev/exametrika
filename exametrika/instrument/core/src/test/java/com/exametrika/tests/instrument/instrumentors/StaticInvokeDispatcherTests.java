/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.StaticInvokeDispatcher;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Invocation;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link StaticInvokeDispatcherTests} are tests for {@link StaticInvokeDispatcher}.
 *
 * @author Medvedev-A
 * @see StaticInvokeDispatcher
 */
public class StaticInvokeDispatcherTests {
    @Test
    public void testDispatcher() throws Throwable {
        InterceptPointcut pointcut = new InterceptPointcut("test", null, Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), false, false, 0);
        IJoinPoint joinPoint1 = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "class", "method", "method", 0, pointcut,
                null, null, null, "file", "debug", 0);
        IJoinPoint joinPoint2 = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 1, 0, "class", "method", "method", 0, pointcut,
                null, null, null, "file", "debug", 1);

        StaticInvokeDispatcher dispatcher = new StaticInvokeDispatcher(Arrays.asList(joinPoint1, joinPoint2));
        IDynamicInterceptor[] interceptors = Tests.get(dispatcher, "interceptors");
        assertThat(interceptors.length, is(2));
        InterceptorMock interceptor1 = ((InterceptorMock) interceptors[0]);
        InterceptorMock interceptor2 = ((InterceptorMock) interceptors[1]);
        assertThat(interceptor1.joinPoint == joinPoint1, is(true));
        assertThat(interceptor2.joinPoint == joinPoint2, is(true));

        Invocation invocation1 = new Invocation();
        dispatcher.invoke(0, 0, invocation1);

        assertThat(interceptor1.intercept == invocation1, is(true));
        assertThat(interceptor2.intercept, nullValue());

        Invocation invocation2 = new Invocation();
        dispatcher.invoke(1, 0, invocation2);

        assertThat(interceptor1.intercept == invocation1, is(true));
        assertThat(interceptor2.intercept == invocation2, is(true));

        dispatcher.invoke(2, 0, invocation1);

        assertThat(interceptor1.intercept == invocation1, is(true));
        assertThat(interceptor2.intercept == invocation2, is(true));
    }

    public static class InterceptorMock implements IDynamicInterceptor {
        public IJoinPoint joinPoint;
        public IInvocation intercept;

        @Override
        public boolean intercept(IInvocation invocation) {
            intercept = invocation;
            return true;
        }

        @Override
        public void start(IJoinPoint joinPoint) {
            this.joinPoint = joinPoint;
        }

        @Override
        public void stop(boolean close) {
            this.intercept = null;
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
