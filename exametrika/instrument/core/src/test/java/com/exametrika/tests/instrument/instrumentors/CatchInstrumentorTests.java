/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.CatchInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestCatchClass;


/**
 * The {@link CatchInstrumentorTests} are tests for {@link CatchInstrumentor}.
 *
 * @author Medvedev-A
 * @see CatchInstrumentor
 */
public class CatchInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestCatchClass.class.getName() + "*"),
                false, null), null);
        CatchPointcut pointcut = new CatchPointcut("test", filter, new TestInterceptorConfiguration(),
                new ClassNameFilter(IllegalArgumentException.class.getName()), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestCatchClass.class.getName(), classTransformer);

        Class<TestCatchClass> clazz = (Class<TestCatchClass>) classLoader.loadClass(TestCatchClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestCatchClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestCatchClass.class.getName(), "testCatch", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.CATCH, pointcut, TestCatchClass.class.getName(), "testCatch", IllegalArgumentException.class.getName(), null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testCatch", int.class).invoke(instance, 1);
        assertThat((Integer) Tests.get(instance, "f3"), is(0));
        assertThat((Integer) Tests.get(instance, "f2"), is(123));

        assertThat(interceptor.intercept == null, is(true));

        Exception exception = (Exception) clazz.getMethod("testCatch", int.class).invoke(instance, 0);
        assertThat((Integer) Tests.get(instance, "f3"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, null, null, null, exception, -1);
    }
}
