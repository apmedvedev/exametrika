/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.fields;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IInstanceContextProvider} represents a provider of instance context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInstanceContextProvider {
    /**
     * Returns instance context.
     *
     * @return instance context or null if context is not set
     */
    JsonObject getContext();

    /**
     * Sets instance context.
     *
     * @param context instance context or null if context is not set
     */
    void setContext(JsonObject context);

    /**
     * Returns extraction time.
     *
     * @return extraction time
     */
    long getExtractionTime();
}
