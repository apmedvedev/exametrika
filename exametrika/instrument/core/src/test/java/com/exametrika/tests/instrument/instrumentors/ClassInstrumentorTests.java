/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.instrument.IInterceptorManager;
import com.exametrika.impl.instrument.InterceptorManager;
import com.exametrika.impl.instrument.config.JoinPoint;
import com.exametrika.impl.instrument.instrumentors.ClassInstrumentor;
import com.exametrika.impl.instrument.instrumentors.InstructionCounter;
import com.exametrika.impl.instrument.instrumentors.MethodInstrumentor;
import com.exametrika.impl.instrument.instrumentors.SkipInstrumentationException;
import com.exametrika.spi.instrument.boot.Interceptors;
import com.exametrika.tests.instrument.instrumentors.data.ITestInterface1;
import com.exametrika.tests.instrument.instrumentors.data.ITestInterface2;
import com.exametrika.tests.instrument.instrumentors.data.ITestInterface3;
import com.exametrika.tests.instrument.instrumentors.data.TestAnnotation1;
import com.exametrika.tests.instrument.instrumentors.data.TestAnnotation2;
import com.exametrika.tests.instrument.instrumentors.data.TestInterceptClass1;
import com.exametrika.tests.instrument.instrumentors.data.TestInterceptClass2;


/**
 * The {@link ClassInstrumentorTests} are tests for {@link ClassInstrumentor}.
 *
 * @author Medvedev-A
 * @see ClassInstrumentor
 */
public class ClassInstrumentorTests {
    @Test
    public void testInstrumentor() throws Throwable {
        InterceptorManager interceptorManager = new InterceptorManager();
        Interceptors.setInvokeDispatcher(interceptorManager);
        interceptorManager.freeAll();

        // Test class name
        InterceptPointcut pointcut = new InterceptPointcut("test", new QualifiedMethodFilter(new ClassFilter(
                new ClassNameFilter("#.*TestInterceptClass.*"), true, null), null), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), false, false, 0);
        IInterceptorManager.JoinPointInfo info1 = interceptorManager.allocate(getClass().getClassLoader(),
                new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, TestInterceptClass2.class.getName(), "test1", "test1", 0, pointcut, null, null, null, null, null, 0));
        assertThat(info1.index, is(0));
        assertThat(info1.version, is(1));
        IInterceptorManager.JoinPointInfo info2 = interceptorManager.allocate(getClass().getClassLoader(),
                new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, TestInterceptClass2.class.getName(), "test2", "test2", 0, pointcut, null, null, null, null, null, 0));
        assertThat(info2.index, is(1));
        assertThat(info2.version, is(1));

        ClassInstrumentor classInstrumentor = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        classInstrumentor.visit(0, 0, Type.getType(TestInterceptClass2.class).getInternalName(), null, Type.getType(TestInterceptClass1.class).getInternalName(),
                new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
        assertThat(classInstrumentor.visitMethod(0, "test", "()V", null, null), instanceOf(InstructionCounter.class));

        // Test free interceptors
        info1 = interceptorManager.allocate(getClass().getClassLoader(),
                new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, TestInterceptClass2.class.getName(), "test1", "test1", 0, pointcut, null, null, null, null, null, 0));
        assertThat(info1.index, is(0));
        assertThat(info1.version, is(3));
        info2 = interceptorManager.allocate(getClass().getClassLoader(),
                new JoinPoint(IJoinPoint.Kind.INTERCEPT, 0, 0, TestInterceptClass2.class.getName(), "test2", "test2", 0, pointcut, null, null, null, null, null, 0));
        assertThat(info2.index, is(1));
        assertThat(info2.version, is(3));

        final ClassInstrumentor classInstrumentor2 = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        new Expected(SkipInstrumentationException.class, new Runnable() {

            @Override
            public void run() {
                classInstrumentor2.visit(0, 0, Type.getType(ITestInterface1.class).getInternalName(), null, Type.getType(Object.class).getInternalName(),
                        new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
                classInstrumentor2.visitMethod(0, "test", "()V", null, null);
            }
        });

        assertThat(classInstrumentor2.visitMethod(0, "test", "()V", null, null), not(instanceOf(MethodInstrumentor.class)));

        // Test annotation
        pointcut = new InterceptPointcut("test", new QualifiedMethodFilter(new ClassFilter(
                null, true, Arrays.asList(new ClassNameFilter(TestAnnotation1.class))), null), Enums.of(InterceptPointcut.Kind.ENTER),
                new TestInterceptorConfiguration(), false, false, 0);

        classInstrumentor = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        classInstrumentor.visit(0, 0, Type.getType(ITestInterface1.class).getInternalName(), null, Type.getType(Object.class).getInternalName(),
                new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
        classInstrumentor.visitAnnotation(Type.getType(TestAnnotation1.class).getDescriptor(), true);
        assertThat(classInstrumentor.visitMethod(0, "test", "()V", null, null), instanceOf(InstructionCounter.class));

        final ClassInstrumentor classInstrumentor3 = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        classInstrumentor3.visit(0, 0, Type.getType(ITestInterface1.class).getInternalName(), null, Type.getType(Object.class).getInternalName(),
                new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
        new Expected(SkipInstrumentationException.class, new Runnable() {

            @Override
            public void run() {
                classInstrumentor3.visitAnnotation(Type.getType(TestAnnotation2.class).getDescriptor(), true);
                classInstrumentor3.visitMethod(0, "test", "()V", null, null);
            }
        });
        assertThat(classInstrumentor3.visitMethod(0, "test", "()V", null, null), not(instanceOf(InstructionCounter.class)));

        // Test method name
        pointcut = new InterceptPointcut("test", new QualifiedMethodFilter(null, new MemberFilter(new MemberNameFilter("test(*"), null)),
                Enums.of(InterceptPointcut.Kind.ENTER), new TestInterceptorConfiguration(), false, false, 0);

        classInstrumentor = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        classInstrumentor.visit(0, 0, Type.getType(ITestInterface1.class).getInternalName(), null, Type.getType(Object.class).getInternalName(),
                new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
        classInstrumentor.visitAnnotation(Type.getType(TestAnnotation1.class).getDescriptor(), true);
        assertThat(classInstrumentor.visitMethod(0, "test", "()V", null, null), instanceOf(InstructionCounter.class));

        classInstrumentor = new ClassInstrumentor(new EmptyClassVisitor(), interceptorManager, getClass().getClassLoader(),
                new HashSet(Collections.<Pointcut>asSet(pointcut)), null, null, new HashSet<String>());
        classInstrumentor.visit(0, 0, Type.getType(ITestInterface1.class).getInternalName(), null, Type.getType(Object.class).getInternalName(),
                new String[]{Type.getType(ITestInterface2.class).getInternalName(), Type.getType(ITestInterface3.class).getInternalName()});
        classInstrumentor.visitAnnotation(Type.getType(TestAnnotation2.class).getDescriptor(), true);
        assertThat(classInstrumentor.visitMethod(0, "test1", "()V", null, null), not(instanceOf(InstructionCounter.class)));
    }
}
