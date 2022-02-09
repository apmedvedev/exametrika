/**
 * Copyright 2011 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.spi.instrument.boot.IInvocation;
import com.exametrika.spi.instrument.boot.Invocation;
import com.exametrika.spi.instrument.intercept.IDynamicInterceptor;


public class InterceptorMock implements IDynamicInterceptor {
    public IInvocation intercept;
    public IInvocation enter;
    public IInvocation returnExit;
    public IInvocation throwExit;
    public IJoinPoint joinPoint;

    @Override
    public boolean intercept(IInvocation invocation) {
        invocation = clone(invocation);
        switch (invocation.getKind()) {
            case INTERCEPT:
                intercept = invocation;
                break;
            case ENTER:
                enter = invocation;
                break;
            case RETURN_EXIT:
                returnExit = invocation;
                break;
            case THROW_EXIT:
                throwExit = invocation;
                break;
        }

        return true;
    }

    @Override
    public void start(IJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }

    @Override
    public void stop(boolean close) {
        this.intercept = null;
        this.enter = null;
        this.returnExit = null;
        this.throwExit = null;
    }

    private IInvocation clone(IInvocation invocation) {
        Invocation copy = new Invocation();
        copy.kind = invocation.getKind();
        copy.instance = invocation.getThis();
        copy.object = invocation.getObject();
        copy.params = invocation.getParams();
        copy.value = invocation.getValue();
        copy.exception = invocation.getException();
        copy.index = invocation.getIndex();

        return copy;
    }
}
