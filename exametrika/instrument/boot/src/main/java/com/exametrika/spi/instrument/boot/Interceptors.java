/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;

import java.util.Arrays;
import java.util.ServiceLoader;


/**
 * The {@link Interceptors} represents an interceptors utils.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Interceptors extends StaticInterceptor {
    private static ThreadLocal<Context> context = new ThreadLocal<Context>();
    private static boolean guard;
    private static volatile boolean[] flags = new boolean[10000];
    private static volatile IInvokeDispatcher invokeDispatcher;

    static {
        ServiceLoader<IInvokeDispatcherFactory> serviceLoader = ServiceLoader.<IInvokeDispatcherFactory>load(IInvokeDispatcherFactory.class, IInvokeDispatcherFactory.class.getClassLoader());
        if (serviceLoader.iterator().hasNext()) {
            IInvokeDispatcherFactory dispatcherFactory = serviceLoader.iterator().next();
            setInvokeDispatcher(dispatcherFactory.createDispatcher());
        }
    }

    public static void setInvokeDispatcher(IInvokeDispatcher invokeDispatcher) {
        Interceptors.invokeDispatcher = invokeDispatcher;
        flags = new boolean[10000];
    }

    public static void enableAll() {
        flags = new boolean[10000];
    }

    public static void onLine(int index, int version, Object instance) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
        }
    }

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        if (!isEnabled(index))
            return null;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.ENTER;
            invocation.instance = instance;
            invocation.params = params;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.params = null;
        }

        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.RETURN_EXIT;
            invocation.instance = instance;
            invocation.value = retVal;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.value = null;
        }
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.THROW_EXIT;
            invocation.instance = instance;
            invocation.exception = exception;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.exception = null;
        }
    }

    public static void onCatch(int index, int version, Object instance, Throwable exception) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.exception = exception;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.exception = null;
        }
    }

    public static void onMonitorBeforeEnter(int index, int version, Object instance, Object monitor) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = monitor;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static void onMonitorAfterEnter(int index, int version, Object instance, Object monitor) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = monitor;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static void onMonitorBeforeExit(int index, int version, Object instance, Object monitor) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = monitor;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static void onMonitorAfterExit(int index, int version, Object instance, Object monitor) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = monitor;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        if (!isEnabled(index))
            return null;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.ENTER;
            invocation.instance = instance;
            invocation.object = callee;
            invocation.params = params;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.params = null;
        }

        return null;
    }

    public static void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.RETURN_EXIT;
            invocation.instance = instance;
            invocation.object = callee;
            invocation.value = retVal;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.value = null;
        }
    }

    public static void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.THROW_EXIT;
            invocation.instance = instance;
            invocation.object = callee;
            invocation.exception = exception;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.exception = null;
        }
    }

    public static void onThrow(int index, int version, Object instance, Throwable exception) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.exception = exception;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.exception = null;
        }
    }

    public static void onNewObject(int index, int version, Object instance, Object object) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = object;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static void onNewArray(int index, int version, Object instance, Object array) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = array;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
        }
    }

    public static void onFieldGet(int index, int version, Object instance, Object fieldOwner, Object fieldValue) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = fieldOwner;
            invocation.value = fieldValue;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.value = null;
        }
    }

    public static void onFieldSet(int index, int version, Object instance, Object fieldOwner, Object newFieldValue) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = fieldOwner;
            invocation.value = newFieldValue;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.value = null;
        }
    }

    public static void onArrayGet(int index, int version, Object instance, Object array, int elementIndex, Object elementValue) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = array;
            invocation.value = elementValue;
            invocation.index = elementIndex;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.value = null;
            invocation.index = -1;
        }
    }

    public static void onArraySet(int index, int version, Object instance, Object array, int elementIndex, Object newElementValue) {
        if (!isEnabled(index))
            return;

        Context context = ensureContext();
        if (context != null && !context.inCall) {
            Invocation invocation = context.invocation;
            invocation.kind = IInvocation.Kind.INTERCEPT;
            invocation.instance = instance;
            invocation.object = array;
            invocation.value = newElementValue;
            invocation.index = elementIndex;

            invoke(index, version, context);

            invocation.kind = null;
            invocation.instance = null;
            invocation.object = null;
            invocation.value = null;
            invocation.index = -1;
        }
    }

    private Interceptors() {
    }

    private static boolean isEnabled(int index) {
        IInvokeDispatcher invokeDispatcher = Interceptors.invokeDispatcher;
        if (invokeDispatcher == null)
            return false;

        boolean[] flags = Interceptors.flags;
        if (index < flags.length) {
            if (flags[index])
                return false;
        }

        return true;
    }

    private static void invoke(int index, int version, Context context) {
        IInvokeDispatcher invokeDispatcher = Interceptors.invokeDispatcher;
        if (invokeDispatcher != null) {
            context.inCall = true;

            try {
                if (!invokeDispatcher.invoke(index, version, context.invocation))
                    disable(index);
            } catch (Exception e) {
            }

            context.inCall = false;
        }
    }

    private static void disable(int index) {
        boolean[] flags = Interceptors.flags;
        if (index < flags.length)
            flags[index] = true;
        else
            growDisable(index);
    }

    private static synchronized void growDisable(int index) {
        int newCapacity = (Math.max(Interceptors.flags.length, index) * 3) / 2 + 1;

        Interceptors.flags = Arrays.copyOf(Interceptors.flags, newCapacity);
        Interceptors.flags[index] = true;
    }

    private static Context ensureContext() {
        Context context = Interceptors.context.get();
        if (context != null)
            return context;

        return createContext();
    }

    private static synchronized Context createContext() {
        if (guard)
            return null;

        guard = true;
        Context context = new Context();
        Interceptors.context.set(context);
        guard = false;

        return context;
    }

    private static class Context {
        public Invocation invocation = new Invocation();
        public boolean inCall;
    }
}
