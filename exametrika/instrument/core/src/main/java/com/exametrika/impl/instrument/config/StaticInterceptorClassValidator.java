/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link StaticInterceptorClassValidator} is used to validate class structure of static interceptor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StaticInterceptorClassValidator {
    private static final Map<String, List<String>> pointcutMethods = new HashMap<String, List<String>>();

    static {
        pointcutMethods.put(ArrayGetPointcut.class.getName(), Arrays.<String>asList(
                "onArrayGet"));
        pointcutMethods.put(ArraySetPointcut.class.getName(), Arrays.<String>asList(
                "onArraySet"));
        pointcutMethods.put(CallPointcut.class.getName(), Arrays.<String>asList(
                "onCallEnter", "onCallReturnExit", "onCallThrowExit"));
        pointcutMethods.put(CatchPointcut.class.getName(), Arrays.<String>asList(
                "onCatch"));
        pointcutMethods.put(FieldGetPointcut.class.getName(), Arrays.<String>asList(
                "onFieldGet"));
        pointcutMethods.put(FieldSetPointcut.class.getName(), Arrays.<String>asList(
                "onFieldSet"));
        pointcutMethods.put(InterceptPointcut.class.getName(), Arrays.<String>asList(
                "onEnter", "onReturnExit", "onThrowExit"));
        pointcutMethods.put(LinePointcut.class.getName(), Arrays.<String>asList(
                "onLine"));
        pointcutMethods.put(MonitorInterceptPointcut.class.getName(), Arrays.<String>asList(
                "onMonitorBeforeEnter", "onMonitorAfterEnter", "onMonitorBeforeExit", "onMonitorAfterExit"));
        pointcutMethods.put(NewArrayPointcut.class.getName(), Arrays.<String>asList(
                "onNewArray"));
        pointcutMethods.put(NewObjectPointcut.class.getName(), Arrays.<String>asList(
                "onNewObject"));
        pointcutMethods.put(ThrowPointcut.class.getName(), Arrays.<String>asList(
                "onThrow"));
    }

    public boolean validate(String pointcutClassName, Class clazz) {
        List<String> methods = pointcutMethods.get(pointcutClassName);
        if (methods == null)
            return false;

        for (String method : methods) {
            if (!validateMethod(method, clazz))
                return false;
        }

        return true;
    }

    private boolean validateMethod(String methodName, Class clazz) {
        Method method = getMethod(methodName, clazz);
        if (method == null)
            return false;

        Method ethalon = getMethod(methodName, StaticInterceptor.class);
        return compare(method, ethalon);
    }

    private boolean compare(Method method, Method ethalon) {
        if (!method.getReturnType().equals(ethalon.getReturnType()))
            return false;

        return Arrays.equals(method.getParameterTypes(), ethalon.getParameterTypes());
    }

    private Method getMethod(String methodName, Class clazz) {
        for (Method method : clazz.getMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers))
                continue;

            if (method.getName().equals(methodName))
                return method;
        }

        return null;
    }
}
