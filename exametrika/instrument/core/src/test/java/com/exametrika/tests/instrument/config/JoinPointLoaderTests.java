/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.impl.instrument.config.JoinPointLoader;
import com.exametrika.spi.instrument.config.DynamicInterceptorConfiguration;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


/**
 * The {@link JoinPointLoaderTests} are tests for {@link JoinPointLoader}.
 *
 * @author Medvedev-A
 * @see JoinPointLoader
 */
public class JoinPointLoaderTests {
    @Test
    public void testConfigurationLoad() throws Exception {
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        InstrumentationConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(InstrumentationConfiguration.SCHEMA);

        JoinPointLoader joinPointLoader = new JoinPointLoader();
        List<IJoinPoint> joinPoints = new ArrayList<IJoinPoint>(configuration.getPointcuts().size());
        int i = 0;
        for (Pointcut pointcut : configuration.getPointcuts()) {
            JoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, i, 0, "class" + i, "method" + i, "method" + i, 0, pointcut, "calledClass" + i,
                    "calledMember" + i, "calledMember" + i, null, null, i);

            joinPoints.add(joinPoint);
            i++;
        }

        File tmpFile = File.createTempFile("test", "tmp");
        FileOutputStream out = new FileOutputStream(tmpFile);
        joinPointLoader.save(joinPoints, out);
        out.close();

        FileInputStream in = new FileInputStream(tmpFile);
        List<IJoinPoint> joinPoints2 = joinPointLoader.load(in);
        assertThat(joinPoints2, is(joinPoints));
    }

    @Test
    public void testExtensionRegistrar() throws Exception {
        TestInterceptorConfiguration interceptorConfiguration = new TestInterceptorConfiguration("Hello world!!!");
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter((ClassFilter) null, null),
                Enums.of(InterceptPointcut.Kind.ENTER), interceptorConfiguration, false, false, 0);

        JoinPointLoader joinPointLoader = new JoinPointLoader();
        List<IJoinPoint> joinPoints = new ArrayList<IJoinPoint>();
        JoinPoint joinPoint = new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, "class" + 0, "method" + 0, "method" + 0, 0, pointcut, "calledClass" + 0,
                "calledMember" + 0, "calledMember" + 0, null, null, 0);
        joinPoints.add(joinPoint);

        File tmpFile = File.createTempFile("test", "tmp");
        FileOutputStream out = new FileOutputStream(tmpFile);
        joinPointLoader.save(joinPoints, out);
        out.close();

        FileInputStream in = new FileInputStream(tmpFile);
        List<IJoinPoint> joinPoints2 = joinPointLoader.load(in);
        assertThat(joinPoints2, is(joinPoints));
        assertThat(joinPoints2.get(0).getPointcut().getInterceptor(), is((Object) interceptorConfiguration));
    }

    private static String getResourcePath() {
        String className = JoinPointLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }

    public static class TestInterceptorConfiguration extends DynamicInterceptorConfiguration {
        public final String value;

        public TestInterceptorConfiguration(String value) {
            this.value = value;
        }

        @Override
        public IDynamicInterceptor createInterceptor() {
            return null;
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
