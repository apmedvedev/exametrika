/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.profiler.IThreadLocalSlot;

public class TestThreadLocalSlot implements IThreadLocalSlot {
    public Object value;

    @Override
    public <T> T get() {
        return (T) value;
    }
}