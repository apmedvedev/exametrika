/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.intercept;

import com.exametrika.spi.instrument.boot.IInterceptor;


/**
 * The {@link Interceptor} is an default interceptor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Interceptor implements IInterceptor {
    @Override
    public void onLine(int index, int version, Object instance) {
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        return null;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
    }

    @Override
    public void onCatch(int index, int version, Object instance, Throwable exception) {
    }

    @Override
    public void onMonitorBeforeEnter(int index, int version, Object instance, Object monitor) {
    }

    @Override
    public void onMonitorAfterEnter(int index, int version, Object instance, Object monitor) {
    }

    @Override
    public void onMonitorBeforeExit(int index, int version, Object instance, Object monitor) {
    }

    @Override
    public void onMonitorAfterExit(int index, int version, Object instance, Object monitor) {
    }

    @Override
    public Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        return null;
    }

    @Override
    public void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
    }

    @Override
    public void onCallThrowExit(int index, int version, Object param, Object instance, Object callee,
                                Throwable exception) {
    }

    @Override
    public void onThrow(int index, int version, Object instance, Throwable exception) {
    }

    @Override
    public void onNewObject(int index, int version, Object instance, Object object) {
    }

    @Override
    public void onNewArray(int index, int version, Object instance, Object array) {
    }

    @Override
    public void onFieldGet(int index, int version, Object instance, Object fieldOwner, Object fieldValue) {
    }

    @Override
    public void onFieldSet(int index, int version, Object instance, Object fieldOwner, Object newFieldValue) {
    }

    @Override
    public void onArrayGet(int index, int version, Object instance, Object array, int elementIndex, Object elementValue) {
    }

    @Override
    public void onArraySet(int index, int version, Object instance, Object array, int elementIndex,
                           Object newElementValue) {
    }
}
