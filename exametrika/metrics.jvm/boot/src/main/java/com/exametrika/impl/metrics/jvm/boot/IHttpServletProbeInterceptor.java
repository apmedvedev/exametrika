/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.boot;

import java.io.BufferedReader;
import java.io.PrintWriter;

import com.exametrika.spi.instrument.boot.IInterceptor;


/**
 * The {@link IHttpServletProbeInterceptor} represents a static HTTP connection interceptor interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IHttpServletProbeInterceptor extends IInterceptor {
    BufferedReader onReturnExitReader(Object param, Object retVal);

    PrintWriter onReturnExitWriter(Object param, Object retVal);
}
