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
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.NewArrayInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestNewArrayClass;


/**
 * The {@link NewArrayInstrumentorTests} are tests for {@link NewArrayInstrumentor}.
 *
 * @author Medvedev-A
 * @see NewArrayInstrumentor
 */
public class NewArrayInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestNewArrayClass.class.getName() + "*"),
                false, null), null);
        NewArrayPointcut pointcut = new NewArrayPointcut("test", filter, new TestInterceptorConfiguration(), new ClassNameFilter(long.class.getName()), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestNewArrayClass.class.getName(), classTransformer);
        Class<TestNewArrayClass> clazz = (Class<TestNewArrayClass>) classLoader.loadClass(TestNewArrayClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestNewArrayClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestNewArrayClass.class.getName(), "testNew", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.NEW_ARRAY, pointcut, TestNewArrayClass.class.getName(), "testNew", long.class.getName(), null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testNew", int.class).invoke(instance, 0);
        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        assertThat(interceptor.intercept == null, is(true));

        long[] value = (long[]) clazz.getMethod("testNew", int.class).invoke(instance, 1);
        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, value, null, null, null, -1);
    }
}
