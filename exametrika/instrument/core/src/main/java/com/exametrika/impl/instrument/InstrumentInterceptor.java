/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;


/**
 * The {@link InstrumentInterceptor} is a instrument interceptor.
 *
 * @author AndreyM
 * @threadsafety Implementations of this class and its methods are thread safe.
 */
public class InstrumentInterceptor {
    public static InstrumentInterceptor INSTANCE = new InstrumentInterceptor();

    public boolean onBeforeTransform() {
        return false;
    }

    public void onTransformSuccess(int beforeClassSize, int afterClassSize, int joinPointCount) {
    }

    public void onTransformError(String className, Throwable exception) {
    }

    public void onTransformSkip() {
    }

    public void onAfterTransform() {
    }
}
