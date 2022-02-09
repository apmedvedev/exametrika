/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import java.io.InputStream;

import com.exametrika.spi.instrument.boot.IInterceptor;


/**
 * The {@link IHttpConnectionProbeInterceptor} represents a static HTTP connection interceptor interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IHttpConnectionProbeInterceptor extends IInterceptor {
    InputStream onReturnExit(Object param, Object retVal);
}
