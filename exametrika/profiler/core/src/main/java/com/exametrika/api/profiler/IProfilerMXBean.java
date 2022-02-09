/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler;


/**
 * The {@link IProfilerMXBean} represents an profiler MBean.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProfilerMXBean {
    /**
     * Name of MXBean of profiler.
     */
    String MBEAN_NAME = "com.exametrika.profiler:type=Profiler";

    /**
     * Dumps measurements.
     */
    int MEASUREMENTS_FLAG = 0x1;

    /**
     * Dumps main internal state.
     */
    int STATE_FLAG = 0x2;

    /**
     * Dumps full internal state.
     */
    int FULL_STATE_FLAG = STATE_FLAG | 0x4;

    /**
     * Dumps internal state of profiler to specified path.
     *
     * @param flags dump flags
     * @param path  path
     */
    void dump(String path, int flags);
}
