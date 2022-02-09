/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.impl.instrument.FieldInjector;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.StaticClassTransformer;
import com.exametrika.impl.instrument.instrumentors.ArrayGetInstrumentor;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.inject.TestFieldInjectClass1;
import com.exametrika.tests.instrument.instrumentors.inject.TestFieldInjectClass2;


/**
 * The {@link FieldInjectorTests} are tests for {@link FieldInjector}.
 *
 * @author Medvedev-A
 * @see ArrayGetInstrumentor
 */
public class FieldInjectorTests extends AbstractInstrumentorTests {
    @Test
    public void testInject() throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);

        StaticClassTransformer classTransformer = new StaticClassTransformer(interceptorManager, getClass().getClassLoader(),
                new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(),
                        true, new File(tempDir, "transform"), Integer.MAX_VALUE), new File(tempDir, "transform"));
        TestClassLoader classLoader = new TestClassLoader(TestFieldInjectClass1.class.getPackage().getName(), classTransformer);
        Class<TestFieldInjectClass1> clazz1 = (Class<TestFieldInjectClass1>) classLoader.loadClass(TestFieldInjectClass1.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestFieldInjectClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        Class<TestFieldInjectClass2> clazz2 = (Class<TestFieldInjectClass2>) classLoader.loadClass(TestFieldInjectClass2.class.getName());
        assertThat(new File(tempDir, "transform" + File.separator + TestFieldInjectClass2.class.getName().replace('.', '/') + ".class").exists(), is(true));

        Field field11 = clazz1.getField("injectedField1");
        assertThat(Modifier.isPublic(field11.getModifiers()), is(true));
        assertThat(Modifier.isVolatile(field11.getModifiers()), is(true));
        assertThat(Modifier.isTransient(field11.getModifiers()), is(true));
        assertThat(field11.getType() == Object.class, is(true));

        Field field12 = clazz1.getDeclaredField("injectedField2");
        assertThat(Modifier.isPrivate(field12.getModifiers()), is(true));
        assertThat(field12.getType() == String.class, is(true));

        Object instance1 = clazz1.newInstance();
        Tests.set(instance1, "injectedField1", 100);
        Tests.set(instance1, "injectedField2", "test");
        assertThat((Integer) Tests.get(instance1, "injectedField1"), is(100));
        assertThat((String) Tests.get(instance1, "injectedField2"), is("test"));

        Field field21 = clazz2.getField("injectedField1");
        assertThat(Modifier.isPublic(field21.getModifiers()), is(true));
        assertThat(Modifier.isVolatile(field21.getModifiers()), is(true));
        assertThat(Modifier.isTransient(field21.getModifiers()), is(true));
        assertThat(field21.getType() == Object.class, is(true));

        Field field22 = clazz2.getDeclaredField("injectedField2");
        assertThat(Modifier.isPrivate(field22.getModifiers()), is(true));
        assertThat(field22.getType() == String.class, is(true));

        Object instance2 = clazz2.newInstance();
        Tests.set(instance2, "injectedField1", 100);
        Tests.set(instance2, "injectedField2", "test");
        assertThat((Integer) Tests.get(instance2, "injectedField1"), is(100));
        assertThat((String) Tests.get(instance2, "injectedField2"), is("test"));
    }
}
