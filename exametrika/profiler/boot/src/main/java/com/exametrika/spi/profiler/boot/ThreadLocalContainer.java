/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.boot;


/**
 * The {@link ThreadLocalContainer} a container of thread locals.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ThreadLocalContainer implements Runnable {
    public Collector top;
    public boolean inCall;
    public long[] methodCounters;
    public boolean suspended;
    public ThreadLocalContainer subContainer;

    public long[] getCounters() {
        return null;
    }

    public void activateAll() {
    }

    public void deactivateAll() {
    }

    @Override
    public void run() {
    }
}
