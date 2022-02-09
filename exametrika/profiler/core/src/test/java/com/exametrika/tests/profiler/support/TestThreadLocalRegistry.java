/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;

public class TestThreadLocalRegistry implements IThreadLocalProviderRegistry {
    @Override
    public void addProvider(IThreadLocalProvider provider) {
        TestThreadLocalSlot slot = new TestThreadLocalSlot();
        slot.value = provider.allocate();
        provider.setSlot(slot);
    }
}