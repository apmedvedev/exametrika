/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link IDumpProvider} represents a provider of dump of internal state.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDumpProvider {
    /**
     * Returns provider name.
     *
     * @return provider name
     */
    String getName();


    /**
     * Dumps internal state.
     *
     * @param flags dump flags.
     * @return required dump state or null if collector does not have required dump state
     * @see IProfilerMXBean
     */
    JsonObject dump(int flags);
}
