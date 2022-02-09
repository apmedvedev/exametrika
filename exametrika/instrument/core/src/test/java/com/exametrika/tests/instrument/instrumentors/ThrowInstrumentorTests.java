/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.ThrowInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestThrowClass;


/**
 * The {@link ThrowInstrumentorTests} are tests for {@link ThrowInstrumentor}.
 *
 * @author Medvedev-A
 * @see ThrowInstrumentor
 */
public class ThrowInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestThrowClass.class.getName() + "*"),
                false, null), null);
        ThrowPointcut pointcut = new ThrowPointcut("test", filter, new TestInterceptorConfiguration(), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestThrowClass.class.getName(), classTransformer);
        Class<TestThrowClass> clazz = (Class<TestThrowClass>) classLoader.loadClass(TestThrowClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestThrowClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestThrowClass.class.getName(), "testThrow", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.THROW, pointcut, TestThrowClass.class.getName(), "testThrow", null, null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        Exception exception = (Exception) clazz.getMethod("testThrow").invoke(instance);
        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, exception, -1);
    }
}
