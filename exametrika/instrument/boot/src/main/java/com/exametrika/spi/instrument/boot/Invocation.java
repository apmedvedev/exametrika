/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link Invocation} represents an implementation of {@link IInvocation}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class Invocation implements IInvocation {
    public Kind kind;
    public Object instance;
    public Object object;
    public Object[] params;
    public Object value;
    public Throwable exception;
    public int index = -1;
    private Object data;

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public Object getThis() {
        return instance;
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public Object[] getParams() {
        return params;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }
}
