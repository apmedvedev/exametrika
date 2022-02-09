/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

public interface ITestStackClass1 extends Runnable {
    long get();

    void stalled();

    void testLZ4();
}
