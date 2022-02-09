/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.IInvokeDispatcher;
import com.exametrika.spi.instrument.boot.Interceptors;


/**
 * The {@link InterceptorsTests} are tests for {@link Interceptors}.
 *
 * @author Medvedev-A
 * @see Interceptors
 */
public class InterceptorsTests {
    private InvokeDispatcherMock dispatcher = new InvokeDispatcherMock();
    private Object instance = new Object();
    private Object object = new Object();
    private Object[] params = new Object[0];
    private Object value = new Object();
    private Throwable exception = new RuntimeException();

    @Test
    public void testInvokeLine() {
        Interceptors.onLine(10, 11, instance);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onLine(10, 11, instance);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));

        Interceptors.onLine(10, 11, instance);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeEnter() {
        Interceptors.onEnter(10, 11, instance, params);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onEnter(10, 11, instance, params);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.params, is(params));

        Interceptors.onEnter(10, 11, instance, params);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeReturnExit() {
        Interceptors.onReturnExit(10, 11, null, instance, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onReturnExit(10, 11, null, instance, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.value, is(value));
        assertThat(dispatcher.exception, nullValue());

        Interceptors.onReturnExit(10, 11, null, instance, value);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeThrowExit() {
        Interceptors.onThrowExit(10, 11, null, instance, exception);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onThrowExit(10, 11, null, instance, exception);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.value, nullValue());
        assertThat(dispatcher.exception, is(exception));

        Interceptors.onThrowExit(10, 11, null, instance, exception);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeCatch() {
        Interceptors.onCatch(10, 11, instance, exception);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onCatch(10, 11, instance, exception);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.exception, is(exception));

        Interceptors.onCatch(10, 11, instance, exception);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeMonitor() {
        Interceptors.onMonitorBeforeEnter(10, 11, instance, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onMonitorAfterExit(10, 11, instance, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(value));
    }

    @Test
    public void testInvokeEnterCall() {
        Interceptors.onCallEnter(10, 11, instance, value, params);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onCallEnter(10, 11, instance, value, params);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(value));
        assertThat(dispatcher.params, is(params));

        Interceptors.onCallEnter(10, 11, instance, value, params);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeReturnExitCall() {
        Interceptors.onCallReturnExit(10, 11, null, instance, object, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onCallReturnExit(10, 11, null, instance, object, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.value, is(value));
        assertThat(dispatcher.exception, nullValue());

        Interceptors.onCallReturnExit(10, 11, null, instance, object, value);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeThrowExitCall() {
        Interceptors.onCallThrowExit(10, 11, null, instance, object, exception);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onCallThrowExit(10, 11, null, instance, object, exception);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.value, nullValue());
        assertThat(dispatcher.exception, is(exception));

        Interceptors.onCallThrowExit(10, 11, null, instance, object, exception);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeThrow() {
        Interceptors.onThrow(10, 11, instance, exception);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onThrow(10, 11, instance, exception);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.exception, is(exception));

        Interceptors.onThrow(10, 11, instance, exception);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeObjectNew() {
        Interceptors.onNewObject(10, 11, instance, object);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onNewObject(10, 11, instance, object);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));

        Interceptors.onNewObject(10, 11, instance, object);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeAfterArrayNew() {
        Interceptors.onNewArray(10, 11, instance, object);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onNewArray(10, 11, instance, object);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.index, is(-1));

        Interceptors.onNewArray(10, 11, instance, object);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeFieldGet() {
        Interceptors.onFieldGet(10, 11, instance, object, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onFieldGet(10, 11, instance, object, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.value, is(value));

        Interceptors.onFieldGet(10, 11, instance, object, value);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeFieldSet() {
        Interceptors.onFieldSet(10, 11, instance, object, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onFieldSet(10, 11, instance, object, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.value, is(value));

        Interceptors.onFieldSet(10, 11, instance, object, value);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeArrayGet() {
        Interceptors.onArrayGet(10, 11, instance, object, 123, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onArrayGet(10, 11, instance, object, 123, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.index, is(123));
        assertThat(dispatcher.value, is(value));

        Interceptors.onArrayGet(10, 11, instance, object, 123, value);
        assertThat(dispatcher.count, is(2));
    }

    @Test
    public void testInvokeArraySet() {
        Interceptors.onArraySet(10, 11, instance, object, 123, value);
        Interceptors.setInvokeDispatcher(dispatcher);
        Interceptors.onArraySet(10, 11, instance, object, 123, value);

        assertThat(dispatcher.interceptorIndex, is(10));
        assertThat(dispatcher.version, is(11));
        assertThat(dispatcher.count, is(1));
        assertThat(dispatcher.instance, is(instance));
        assertThat(dispatcher.object, is(object));
        assertThat(dispatcher.value, is(value));
        assertThat(dispatcher.index, is(123));

        Interceptors.onArraySet(10, 11, instance, object, 123, value);
        assertThat(dispatcher.count, is(2));
    }

    private static class InvokeDispatcherMock implements IInvokeDispatcher {
        private int interceptorIndex;
        private int version;
        private int count;

        private Object instance;
        private Object object;
        private Object[] params;
        private Object value;
        private Throwable exception;
        private int index = -1;

        @Override
        public boolean invoke(int interceptorIndex, int version, IInvocation invocation) {
            this.interceptorIndex = interceptorIndex;
            this.version = version;
            count++;
            instance = invocation.getThis();
            object = invocation.getObject();
            params = invocation.getParams();
            value = invocation.getValue();
            exception = invocation.getException();
            index = invocation.getIndex();

            Interceptors.onLine(0, 0, null);

            return true;
        }
    }
}
