/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IProbeCollector} represents a measurement collector of probe bound to some particular scope of some particular thread.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IProbeCollector {
    /**
     * Is extraction from outside thread is required (when extraction from probe thread is not performed with sufficient periodicity)?
     *
     * @return true if extraction is required from outside thread
     */
    boolean isExtractionRequired();

    /**
     * Extracts measurements from outside of probe's thread when probe's thread is suspended.
     */
    void extract();

    /**
     * Activates probe when scope is activated.
     */
    void begin();

    /**
     * Deactivates probe when scope is deactivated.
     */
    void end();
}
