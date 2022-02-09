/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.common.json.Json;
import com.exametrika.impl.profiler.boot.AgentStackProbeInterceptor;
import com.exametrika.impl.profiler.boot.AgentlessStackProbeInterceptor;
import com.exametrika.impl.profiler.expression.ProbeExpressionContext;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ScopeContainer;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;


/**
 * The {@link Probes} is an utility class containing various probe helper functions.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Probes {
    public static boolean isInstanceOf(Object instance, String className) {
        if (instance == null)
            return false;

        return isInstanceOf(instance.getClass(), className);
    }

    public static void buildStackTrace(IScope scope, int maxStackTraceDepth, IJoinPointProvider joinPointProvider, Json result) {
        ScopeContainer scopeContainer = ((Scope) scope).getContainer();
        StackTraceElement[] elements = scopeContainer.getParent().getStackTrace();
        Json json = Json.array();

        boolean entryFound = scopeContainer.getParent().thread != Thread.currentThread();
        boolean probeFound = false;
        String errorLocation = null;
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];
            if (entryFound) {
                json.addObject()
                        .put("class", element.getClassName())
                        .put("method", element.getMethodName())
                        .put("file", element.getFileName())
                        .put("line", element.getLineNumber())
                        .end();

                if (errorLocation == null) {
                    if (!joinPointProvider.findJoinPoints(element.getClassName(),
                            element.getMethodName(), ThreadLocalAccessor.underAgent ? AgentStackProbeInterceptor.class :
                                    AgentlessStackProbeInterceptor.class).isEmpty())
                        errorLocation = element.getClassName() + "." + element.getMethodName();
                }

                maxStackTraceDepth--;
                if (maxStackTraceDepth == 0) {
                    if (elements.length - i - 1 > 0)
                        json.addObject().put("more", elements.length - i - 1).end();
                    break;
                }
            } else if (scopeContainer.getContext().isProbe(element.getClassName()))
                probeFound = true;
            else if (probeFound && !scopeContainer.getContext().isProbe(element.getClassName()))
                entryFound = true;
        }
        result.put("stackTrace", json.toArray())
                .putIf("errorLocation", errorLocation, errorLocation != null);
    }

    public static String getErrorLocation(Throwable exception, IJoinPointProvider joinPointProvider) {
        StackTraceElement[] trace = exception.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement element = trace[i];
            if (!joinPointProvider.findJoinPoints(element.getClassName(),
                    element.getMethodName(), ThreadLocalAccessor.underAgent ? AgentStackProbeInterceptor.class :
                            AgentlessStackProbeInterceptor.class).isEmpty())
                return element.getClassName() + "." + element.getMethodName();
        }

        return null;
    }

    public static Map<String, Object> createRuntimeContext(IProbeContext context) {
        Map<String, Object> runtimeContext = new HashMap<String, Object>(MeterExpressions.getRuntimeContext());
        runtimeContext.put("probes", new ProbeExpressionContext(context));

        return runtimeContext;
    }

    private static boolean isInstanceOf(Class clazz, String className) {
        if (clazz.getName().equals(className))
            return true;

        Class superClass = clazz.getSuperclass();
        if (superClass != null && isInstanceOf(superClass, className))
            return true;

        Class[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (isInstanceOf(interfaces[i], className))
                return true;
        }

        return false;
    }

    private Probes() {
    }
}
