/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm.boot;

import com.exametrika.spi.profiler.boot.IBridge;


/**
 * The {@link IHttpServletBridge} represents a HTTP servlet bridge interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IHttpServletBridge extends IBridge {
    String EXA_REQUEST_TIME = "EXA_REQUEST_TIME";
    String EXA_TRACE_TAG = "EXA_TRACE_TAG";

    String getContextName(Object httpServletRequest);

    String getRequestURI(Object httpServletRequest);

    String getQueryString(Object httpServletRequest);

    int getContentLength(Object httpServletRequest);

    String getTag(Object httpServletRequest);

    void setRequestTime(Object httpServletResponse, long time);
}
