/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.boot;

import com.exametrika.spi.instrument.boot.INoTransform;


/**
 * The {@link IBridge} represents a base bridge interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IBridge extends INoTransform {
    /**
     * Does bridge support specified request.
     *
     * @param request request
     * @return true if bridge supports specified request
     */
    boolean supports(Object request);
}
