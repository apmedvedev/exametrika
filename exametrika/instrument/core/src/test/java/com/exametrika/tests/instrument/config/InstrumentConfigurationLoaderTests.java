/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.StaticInterceptor;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link InstrumentConfigurationLoaderTests} are tests for configuration loading.
 *
 * @author Medvedev-A
 */
public class InstrumentConfigurationLoaderTests {
    public static class TestInstrumentationConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.instrumentation", new Pair("classpath:" + Classes.getResourcePath(getClass()) + "/instrument-extension.json", false));
            parameters.typeLoaders.put("TestInterceptor", new TestInterceptorProcessor());
            parameters.typeLoaders.put("TestStaticInterceptor", new TestStaticInterceptorProcessor());
            return parameters;
        }
    }

    public static class TestInterceptor implements IDynamicInterceptor {
        @Override
        public boolean intercept(IInvocation invocation) {
            return true;
        }

        @Override
        public void start(IJoinPoint joinPoint) {
        }

        @Override
        public void stop(boolean close) {
        }
    }

    private static class TestInterceptorConfiguration extends DynamicInterceptorConfiguration {
        @Override
        public IDynamicInterceptor createInterceptor() {
            return new TestInterceptor();
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

    private static class TestStaticInterceptorConfiguration extends StaticInterceptorConfiguration {
        public TestStaticInterceptorConfiguration() {
            super(StaticInterceptor.class);
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

    @Test
    public void testConfigurationLoad() {
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        InstrumentationConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(InstrumentationConfiguration.SCHEMA);

        assertThat(configuration.getRuntimeMode(), is(RuntimeMode.PRODUCTION));
        assertThat(configuration.isDebug(), is(true));
        assertThat(configuration.getDebugPath(), is(new File(System.getProperty("com.exametrika.home") + "/work/instrument/debug")));
        assertThat(configuration.getPointcuts().size(), is(15));
        QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("class"), false, null),
                new MemberFilter(new MemberNameFilter("method"), null));
        QualifiedMemberNameFilter calledFilter = new QualifiedMemberNameFilter(new ClassNameFilter("class"), new MemberNameFilter("method"));
        QualifiedMemberNameFilter fieldFilter = new QualifiedMemberNameFilter(new ClassNameFilter("class"), new MemberNameFilter("field"));
        assertThat(get(configuration.getPointcuts(), 0), is((Pointcut) new ArrayGetPointcut("arrayGet", methodFilter,
                new TestInterceptorConfiguration(), true, false)));
        assertThat(get(configuration.getPointcuts(), 1), is((Pointcut) new ArraySetPointcut("arraySet", methodFilter,
                new TestInterceptorConfiguration(), false, false)));
        assertThat(get(configuration.getPointcuts(), 2), is((Pointcut) new CallPointcut("call", methodFilter,
                new TestInterceptorConfiguration(), calledFilter, true, false, 0)));
        assertThat(get(configuration.getPointcuts(), 3), is((Pointcut) new CatchPointcut("catch", methodFilter,
                new TestInterceptorConfiguration(), new ClassNameFilter("class"), false)));
        assertThat(get(configuration.getPointcuts(), 4), is((Pointcut) new FieldGetPointcut("fieldGet", methodFilter,
                new TestInterceptorConfiguration(), fieldFilter, false, false)));
        assertThat(get(configuration.getPointcuts(), 5), is((Pointcut) new FieldSetPointcut("fieldSet", methodFilter,
                new TestInterceptorConfiguration(), fieldFilter, true, false)));
        assertThat(get(configuration.getPointcuts(), 6), is((Pointcut) new InterceptPointcut("intercept", methodFilter,
                Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT, InterceptPointcut.Kind.THROW_EXIT),
                new TestInterceptorConfiguration(), false, false, 0)));
        assertThat(get(configuration.getPointcuts(), 7), is((Pointcut) new InterceptPointcut("intercept2", methodFilter,
                Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT),
                new TestInterceptorConfiguration(), true, false, 0)));
        assertThat(get(configuration.getPointcuts(), 8), is((Pointcut) new LinePointcut("line", methodFilter,
                new TestInterceptorConfiguration(), 10, 10, false)));
        assertThat(get(configuration.getPointcuts(), 9), is((Pointcut) new MonitorInterceptPointcut("monitorIntercept", methodFilter,
                Enums.of(MonitorInterceptPointcut.Kind.BEFORE_ENTER, MonitorInterceptPointcut.Kind.AFTER_EXIT),
                new TestInterceptorConfiguration(), false)));
        assertThat(get(configuration.getPointcuts(), 10), is((Pointcut) new NewArrayPointcut("newArray", methodFilter,
                new TestInterceptorConfiguration(), new ClassNameFilter("class"), false)));
        assertThat(get(configuration.getPointcuts(), 11), is((Pointcut) new NewObjectPointcut("newObject", methodFilter,
                new TestInterceptorConfiguration(), new ClassNameFilter("class"), false)));
        assertThat(get(configuration.getPointcuts(), 12), is((Pointcut) new ThrowPointcut("throw", methodFilter,
                new TestInterceptorConfiguration(), false)));

        ClassNameFilter className = new ClassNameFilter("class");
        ClassFilter classFilter2 = new ClassFilter(className, false, null);
        ClassFilter classFilter = new ClassFilter(className, true, Arrays.asList(className), Arrays.asList(classFilter2), Arrays.asList(classFilter2));
        MemberNameFilter memberName = new MemberNameFilter("member");
        MemberFilter memberFilter2 = new MemberFilter(memberName, null);
        MemberFilter memberFilter = new MemberFilter(memberName, Arrays.asList(className), Arrays.asList(memberFilter2),
                Arrays.asList(memberFilter2));
        QualifiedMethodFilter methodFilter2 = new QualifiedMethodFilter(classFilter, memberFilter,
                Arrays.asList(methodFilter), Arrays.asList(methodFilter), 10, 20);
        assertThat(get(configuration.getPointcuts(), 13), is((Pointcut) new InterceptPointcut("pointcut1", methodFilter2,
                Enums.of(InterceptPointcut.Kind.ENTER, InterceptPointcut.Kind.RETURN_EXIT),
                new TestInterceptorConfiguration(), true, false, 0)));
        Pointcut pointcut = get(configuration.getPointcuts(), 13);
        assertThat(pointcut.getInterceptor(), instanceOf(TestInterceptorConfiguration.class));

        ClassNameFilter className2 = new ClassNameFilter("class", Arrays.asList(className), Arrays.asList(className));
        MemberNameFilter memberName2 = new MemberNameFilter("member", Arrays.asList(memberName), Arrays.asList(memberName));
        QualifiedMemberNameFilter fieldFilter2 = new QualifiedMemberNameFilter(className2, memberName2, Arrays.asList(fieldFilter),
                Arrays.asList(fieldFilter));
        assertThat(get(configuration.getPointcuts(), 14), is((Pointcut) new FieldGetPointcut("pointcut2", methodFilter,
                new TestInterceptorConfiguration(), fieldFilter2, false, false)));
    }

    private Pointcut get(Set<Pointcut> pointcuts, int index) {
        int i = 0;
        for (Pointcut pointcut : pointcuts) {
            if (i == index)
                return pointcut;

            i++;
        }

        return null;
    }

    private static String getResourcePath() {
        String className = InstrumentConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }

    private static class TestInterceptorProcessor implements IExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object element, ILoadContext context) {
            return new TestInterceptorConfiguration();
        }

        @Override
        public void setExtensionLoader(IExtensionLoader extensionProcessor) {
        }
    }

    private static class TestStaticInterceptorProcessor implements IExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object element, ILoadContext context) {
            return new TestStaticInterceptorConfiguration();
        }

        @Override
        public void setExtensionLoader(IExtensionLoader extensionProcessor) {
        }
    }
}
