/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.intercept;


/**
 * The {@link IValueMXBean} is a MX Bean interface for getting value.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IValueMXBean {
    /**
     * Returns value.
     *
     * @return value
     */
    long getValue();
}
