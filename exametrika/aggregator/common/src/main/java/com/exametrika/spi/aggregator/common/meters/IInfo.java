/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;


/**
 * The {@link IInfo} represents a informational meter.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IInfo extends IMeter {
    /**
     * Sets informational value.
     *
     * @param value informational value of one of the supported Json types.
     */
    @Override
    void measure(Object value);
}
