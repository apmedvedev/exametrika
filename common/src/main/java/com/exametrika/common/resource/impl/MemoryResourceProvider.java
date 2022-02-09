/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.utils.Memory;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * The {@link MemoryResourceProvider} is a resource provider which uses all available memory of process as a resource.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MemoryResourceProvider implements IResourceProvider {
    private final boolean nativeMemory;

    public MemoryResourceProvider(boolean nativeMemory) {
        this.nativeMemory = nativeMemory;
    }

    @Override
    public long getAmount() {
        long processAmount;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        if (nativeMemory)
            processAmount = Memory.getMaxDirectMemory();
        else
            processAmount = memoryBean.getHeapMemoryUsage().getMax();

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return Math.min(osBean.getTotalPhysicalMemorySize(), processAmount);
    }
}
