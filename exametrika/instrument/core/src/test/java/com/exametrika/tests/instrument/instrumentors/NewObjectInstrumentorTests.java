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
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.NewObjectInstrumentor;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestNewObjectClass;


/**
 * The {@link NewObjectInstrumentorTests} are tests for {@link NewObjectInstrumentor}.
 *
 * @author Medvedev-A
 * @see NewObjectInstrumentor
 */
public class NewObjectInstrumentorTests extends AbstractInstrumentorTests {
    @Test
    public void testIntercept() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        QualifiedMethodFilter filter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(TestNewObjectClass.class.getName() + "*"),
                false, null), null);
        NewObjectPointcut pointcut = new NewObjectPointcut("test", filter, new TestInterceptorConfiguration(), new ClassNameFilter(Integer.class.getName()), false);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(pointcut),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestNewObjectClass.class.getName(), classTransformer);
        Class<TestNewObjectClass> clazz = (Class<TestNewObjectClass>) classLoader.loadClass(TestNewObjectClass.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestNewObjectClass.class.getName().replace('.', '/') + ".class").exists(), is(true));

        InterceptorMock interceptor = getInterceptor(pointcut, TestNewObjectClass.class.getName(), "testNew", interceptorManager);
        checkJoinPoint(interceptor.joinPoint, IJoinPoint.Kind.NEW_OBJECT, pointcut, TestNewObjectClass.class.getName(), "testNew", Integer.class.getName(), null);

        assertThat(interceptor.intercept == null, is(true));

        Object instance = clazz.newInstance();
        clazz.getMethod("testNew", int.class).invoke(instance, 0);
        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        assertThat(interceptor.intercept == null, is(true));

        Integer value = (Integer) clazz.getMethod("testNew", int.class).invoke(instance, 1);
        assertThat((Integer) Tests.get(instance, "f1"), is(123));

        checkInvocation(interceptor.intercept, IInvocation.Kind.INTERCEPT, instance, value, null, null, null, -1);
    }
}
