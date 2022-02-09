/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentorAdapter;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.TestAnnotation1;
import com.exametrika.tests.instrument.instrumentors.data.TestAnnotation2;


/**
 * The {@link MethodInstrumentorTests} are tests for {@link MethodInstrumentorAdapter}.
 *
 * @author Medvedev-A
 * @see MethodInstrumentorAdapter
 */
public class MethodInstrumentorTests {
    @Test
    public void testInstrumentor() throws Throwable {
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);

        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter(null, new MemberFilter(
                null, Arrays.asList(new ClassNameFilter(TestAnnotation1.class)))), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), false, false, 0);

        MethodInstrumentor methodInstrumentor = new MethodInstrumentor(interceptorManager, "Test", "Object", Collections.<String>asSet(),
                Collections.<String>asSet(), "test", "test", 0, "()V", new EmptyMethodVisitor(),
                new LinkedHashSet(Arrays.<Pointcut>asList(pointcut)), null, null, null, null, null, 0, null, null);

        methodInstrumentor.visitAnnotation(Type.getType(TestAnnotation2.class).getDescriptor(), true);
        methodInstrumentor.visitCode();
        assertThat(((Set) Tests.get(methodInstrumentor, "pointcuts")).isEmpty(), is(true));
        assertThat((Boolean) Tests.get(methodInstrumentor, "disabled"), is(true));
    }
}
