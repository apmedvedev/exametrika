/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm.boot;

import com.exametrika.spi.profiler.boot.IBridge;


/**
 * The {@link IJmsConsumerBridge} represents a JMS consumer bridge interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJmsConsumerBridge extends IBridge {
    String getDestinationName(Object message);

    String getDestinationType(Object message);

    String getTag(Object message);

    int getSize(Object message);

    Object getProperty(String name, Object message);
}
