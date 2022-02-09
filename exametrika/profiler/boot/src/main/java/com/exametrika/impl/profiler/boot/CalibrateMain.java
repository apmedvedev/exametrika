/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.boot;

/**
 * The {@link CalibrateMain} is a calibration stub.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class CalibrateMain {
    public static void main(String[] args) {
        try {
            while (!MainInterceptor.calibrated)
                Thread.sleep(100);
        } catch (Throwable e) {
        }
    }
}
