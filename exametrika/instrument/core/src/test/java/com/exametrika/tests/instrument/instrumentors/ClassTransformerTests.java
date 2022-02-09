/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.IConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.log.config.LoggingConfiguration;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.boot.utils.PathClassLoader;
import com.exametrika.impl.instrument.ClassTransformer;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.tests.instrument.instrumentors.data.TestInterceptClass1;


/**
 * The {@link ClassTransformerTests} are tests for {@link ClassTransformer}.
 *
 * @author Medvedev-A
 * @see ClassTransformer
 */
public class ClassTransformerTests {
    @After
    public void tearDown() {
        System.clearProperty("com.exametrika.instrument.allowAllTransformations");
    }

    @Test
    public void testTransform() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InstrumentConfigurationLoaderMock loader = new InstrumentConfigurationLoaderMock();
        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(),
                true, new File(tempDir, "transform"), Integer.MAX_VALUE);

        InstrumentationMock instrumentation = new InstrumentationMock();
        InterceptorManager interceptorManager = new InterceptorManager();

        ClassTransformer transformer = new ClassTransformer(instrumentation, interceptorManager, null, null, new HashMap());
        ByteOutputStream out = new ByteOutputStream();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(TestInterceptClass1.class.getName().replace('.', '/') + ".class");
        IOs.copy(stream, out);
        IOs.close(stream);

        byte[] transformed = transformer.transform(null, TestInterceptClass1.class.getName().replace('.', '/'), TestInterceptClass1.class, null, out.toByteArray());
        assertThat(transformed, nullValue());

        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(new InterceptPointcut("test",
                new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter(Arrays.asList("*"), Arrays.asList("java.*", "sun.*")), false, null), null),
                Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0)),
                true, new File(tempDir, "transform"), Integer.MAX_VALUE);
        transformer.setConfiguration(loader.configuration);

        out = new ByteOutputStream();
        stream = getClass().getClassLoader().getResourceAsStream(TestInterceptClass1.class.getName().replace('.', '/') + ".class");
        IOs.copy(stream, out);
        IOs.close(stream);

        transformed = transformer.transform(null, TestInterceptClass1.class.getName().replace('.', '/'), TestInterceptClass1.class, null, out.toByteArray());
        assertThat(transformed != null, is(true));

        assertThat(new File(tempDir, "transform" + File.separator + TestInterceptClass1.class.getName().replace('.', '/') + ".class").exists(), is(true));

        transformed = transformer.transform(null, ThreadLocal.class.getName().replace('.', '/'), ThreadLocal.class, null, out.toByteArray());
        assertThat(transformed, nullValue());

        transformed = transformer.transform(null, Reference.class.getName().replace('.', '/'), Reference.class, null, out.toByteArray());
        assertThat(transformed, nullValue());

        transformed = transformer.transform(null, CharSequence.class.getName().replace('.', '/'), CharSequence.class, null, out.toByteArray());
        assertThat(transformed, nullValue());

        PathClassLoader pathClassLoader = new PathClassLoader(java.util.Collections.<File>emptyList(), java.util.Collections.<File>emptyList(), Arrays.asList(TestInterceptClass1.class.getPackage().getName()),
                TestInterceptClass1.class.getClassLoader());
        transformed = transformer.transform(pathClassLoader, TestInterceptClass1.class.getName().replace('.', '/'),
                pathClassLoader.loadClass(TestInterceptClass1.class.getName()), null, out.toByteArray());
        assertThat(transformed, nullValue());

        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(new InterceptPointcut("test", new QualifiedMethodFilter(
                new ClassFilter(new ClassNameFilter(Long.class), false, null), null),
                Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0)),
                false, null, Integer.MAX_VALUE);
        transformer.setConfiguration(loader.configuration);
        transformed = transformer.transform(null, TestInterceptClass1.class.getName().replace('.', '/'), TestInterceptClass1.class, null, out.toByteArray());
        assertThat(transformed, nullValue());
    }

    @Test
    public void testRetransform() throws Exception {
        System.setProperty("com.exametrika.instrument.allowAllTransformations", "true");
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InstrumentationMock instrumentation = new InstrumentationMock();
        InterceptorManagerMock interceptorManager = new InterceptorManagerMock();
        InstrumentConfigurationLoaderMock loader = new InstrumentConfigurationLoaderMock();
        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(),
                true, new File(tempDir, "transform"), Integer.MAX_VALUE);
        ClassTransformer transformer = new ClassTransformer(instrumentation, interceptorManager, null, null, new HashMap());

        instrumentation.retransformSupported = false;
        transformer.setConfiguration(loader.configuration);
        assertThat(Tests.get(transformer, "configuration"), nullValue());

        TestClassLoader loader2 = new TestClassLoader(null, getClass().getClassLoader(), Classes.getPackageName(TestInterceptClass1.class.getName()), null);
        Class testClass = loader2.loadClass(TestInterceptClass1.class.getName());
        instrumentation.retransformSupported = true;
        instrumentation.loadedClasses.add(String.class);
        instrumentation.loadedClasses.add(Integer.class);
        instrumentation.loadedClasses.add(testClass);
        instrumentation.loadedClasses.add(CharSequence.class);
        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(new InterceptPointcut("test", null,
                Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0)),
                false, null, Integer.MAX_VALUE);
        transformer.setConfiguration(loader.configuration);
        assertThat(Tests.get(transformer, "configuration") == loader.configuration, is(true));
        assertThat(instrumentation.retransformedClasses, is((List) Arrays.asList(String.class, Integer.class, testClass)));

        instrumentation.retransformedClasses.clear();
        transformer.setConfiguration(loader.configuration);
        assertThat(instrumentation.retransformedClasses.isEmpty(), is(true));

        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(
                new InterceptPointcut("test", new QualifiedMethodFilter(
                        new ClassFilter(new ClassNameFilter(Long.class), false, null), null),
                        Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0)),
                false, null, Integer.MAX_VALUE);
        instrumentation.loadedClasses.add(Long.class);
        transformer.setConfiguration(loader.configuration);
        assertThat(instrumentation.retransformedClasses, is((List) Arrays.asList(String.class, Integer.class, testClass, Long.class)));
        instrumentation.retransformedClasses.clear();
        assertThat(interceptorManager.freedInterceptors, is((Set) new HashSet(Arrays.asList(new Pair(null, String.class.getName()), new Pair(null, Long.class.getName()),
                new Pair(null, Integer.class.getName()), new Pair(testClass.getClassLoader(), testClass.getName())))));
        interceptorManager.freedInterceptors.clear();

        Pointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter(
                new ClassFilter(new ClassNameFilter(Long.class), false, null), null),
                Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0);
        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT,
                Collections.<Pointcut>asSet(pointcut), false, null, Integer.MAX_VALUE);
        transformer.setConfiguration(loader.configuration);

        assertThat(instrumentation.retransformedClasses.isEmpty(), is(true));

        transformer.onTimer();
        assertThat(interceptorManager.freeUnloaded, is(true));

        assertThat(interceptorManager.freeAll, is(false));
        transformer.close(false);
        assertThat(interceptorManager.freedInterceptors, is((Set) new HashSet(Arrays.asList(new Pair(null, Long.class.getName())))));
        assertThat(interceptorManager.freeAll, is(true));
        assertThat(instrumentation.retransformedClasses, is((List) Arrays.asList(Long.class)));
        assertThat(Tests.get(transformer, "configuration"), nullValue());
    }

    @Test
    public void testExtension() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        InstrumentationMock instrumentation = new InstrumentationMock();
        InterceptorManagerMock interceptorManager = new InterceptorManagerMock();
        InstrumentConfigurationLoaderMock loader = new InstrumentConfigurationLoaderMock();
        loader.configuration = new InstrumentationConfiguration(RuntimeMode.DEVELOPMENT, Collections.<Pointcut>asSet(),
                true, new File(tempDir, "transform"), Integer.MAX_VALUE);
        ClassTransformer transformer = new ClassTransformer(instrumentation, interceptorManager, null, null, new HashMap());

        instrumentation.retransformSupported = true;
        transformer.setConfiguration(loader.configuration);

        transformer.close(false);
    }

    private static class InstrumentConfigurationLoaderMock implements IConfigurationLoader {
        public InstrumentationConfiguration configuration;

        @Override
        public ILoadContext loadConfiguration(String configurationFileName) {
            LoadContextMock context = new LoadContextMock(configuration);
            return context;
        }
    }

    private static class LoadContextMock implements ILoadContext {
        private final InstrumentationConfiguration configuration;

        public LoadContextMock(InstrumentationConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public <T> T get(String configurationType) {
            if (configurationType.equals(InstrumentationConfiguration.SCHEMA))
                return (T) configuration;
            else if (configurationType.equals(LoggingConfiguration.SCHEMA))
                return (T) new LoggingConfiguration();
            else
                return null;
        }

        @Override
        public <T> T findParameter(String name) {
            return null;
        }

        @Override
        public void setParameter(String name, Object value) {
        }
    }

    private static class InterceptorManagerMock implements IInterceptorManager {
        private boolean freeAll;
        private boolean freeUnloaded;
        private Set<Pair<ClassLoader, String>> freedInterceptors = new HashSet<Pair<ClassLoader, String>>();

        @Override
        public JoinPointInfo allocate(ClassLoader classLoader, IJoinPoint joinPoint) {
            return null;
        }

        @Override
        public void free(ClassLoader classLoader, String className) {
            freedInterceptors.add(new Pair(classLoader, className));
        }

        @Override
        public void freeAll() {
            this.freeAll = true;
        }

        @Override
        public void freeUnloaded() {
            this.freeUnloaded = true;
        }

        @Override
        public int getJoinPointCount() {
            return 0;
        }
    }
}
