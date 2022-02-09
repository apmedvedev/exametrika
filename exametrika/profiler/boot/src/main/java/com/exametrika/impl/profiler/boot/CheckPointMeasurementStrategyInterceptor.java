/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link CheckPointMeasurementStrategyInterceptor} represents a static interceptor of checkpoint measurement strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CheckPointMeasurementStrategyInterceptor extends StaticInterceptor {
    public static boolean allowed;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        allowed = true;
        return null;
    }
}
