/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


/**
 * The {@link IStackProbeCollectorFactory} represents a factory of {@link StackProbeCollector}.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IStackProbeCollectorFactory {
    /**
     * Creates a new stack counter collector.
     *
     * @param index   collector's numeric identifier
     * @param version collector's join point version
     * @param parent  parent
     * @param param   custom user defined parameter
     * @return stack counter collector
     */
    StackProbeCollector createCollector(int index, int version, StackProbeCollector parent, Object param);
}
