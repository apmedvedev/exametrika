/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

import com.exametrika.spi.instrument.boot.StaticInterceptor;


/**
 * The {@link MainInterceptor} represents a static interceptor of main method.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MainInterceptor extends StaticInterceptor {
    public static volatile boolean calibrated;

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        try {
            while (!calibrated)
                Thread.sleep(100);
        } catch (Throwable e) {
        }

        return null;
    }
}
