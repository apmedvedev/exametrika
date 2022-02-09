/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.tests.Tests;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.impl.instrument.instrumentors.AbstractInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;


/**
 * The {@link AbstractInstrumentorTests} are tests for {@link AbstractInstrumentor} implementations.
 *
 * @author Medvedev-A
 * @see AbstractInstrumentor
 */
public abstract class AbstractInstrumentorTests {
    protected final InterceptorMock getInterceptor(Pointcut pointcut, String className, String methodName, IInterceptorManager interceptorManager) throws Exception {
        return getInterceptor(pointcut, className, methodName, 0, interceptorManager);
    }

    protected final InterceptorMock getInterceptor(Pointcut pointcut, String className, String methodName, int index,
                                                   IInterceptorManager interceptorManager) throws Exception {
        int i = 0;
        Object[] list = (Object[]) (Tests.get(Tests.get(interceptorManager, "entries"), "elements"));
        for (Object o : list) {
            InterceptorMock interceptor = Tests.get(o, "interceptor");
            if (interceptor != null && interceptor.joinPoint.getPointcut().equals(pointcut) &&
                    interceptor.joinPoint.getClassName().equals(className) &&
                    interceptor.joinPoint.getMethodName().equals(methodName)) {
                if (i == index)
                    return interceptor;
                else
                    i++;
            }
        }

        return null;
    }

    protected final void checkJoinPoint(IJoinPoint joinPoint, IJoinPoint.Kind kind, Pointcut pointcut, String className, String methodName, String calledClassName,
                                        String calledMethodName) {
        assertThat(joinPoint.getKind(), is(kind));
        assertThat(joinPoint.getClassName(), is(className));
        assertThat(joinPoint.getMethodName(), is(methodName));
        assertThat(joinPoint.getPointcut(), is(pointcut));
        assertThat(joinPoint.getCalledClassName(), is(calledClassName));
        assertThat(joinPoint.getCalledMemberName(), is(calledMethodName));
    }

    protected final void checkInvocation(IInvocation invocation, IInvocation.Kind kind, Object instance, Object object, Object[] params, Object value, Throwable exception, int index) {
        assertThat(invocation != null, is(true));
        assertThat(invocation.getKind(), is(kind));
        assertThat(invocation.getThis() == instance, is(true));
        assertThat(invocation.getObject() == object, is(true));
        assertThat(Arrays.equals(invocation.getParams(), params), is(true));
        assertThat(invocation.getValue(), is(value));
        assertThat(invocation.getException() == exception, is(true));
        assertThat(invocation.getIndex() == index, is(true));
    }
}
