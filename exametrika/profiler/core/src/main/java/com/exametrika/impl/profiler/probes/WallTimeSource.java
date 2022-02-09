/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.spi.profiler.ITimeSource;


/**
 * The {@link WallTimeSource} is a wall clock time source.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class WallTimeSource implements ITimeSource {
    @Override
    public long getCurrentTime() {
        return System.nanoTime();
    }

    @Override
    public long getCurrentTime(long threadId) {
        return System.nanoTime();
    }
}
