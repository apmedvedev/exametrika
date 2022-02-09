/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm.boot;

import com.exametrika.spi.profiler.boot.IBridge;


/**
 * The {@link IJmsProducerBridge} represents a JMS producer bridge interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJmsProducerBridge extends IBridge {
    boolean isJmsProducer(Object instance);

    boolean isMessage(Object instance);

    boolean isBytesOrStreamMessage(Object instance);

    boolean isTextMessage(Object instance);

    boolean isMapMessage(Object instance);

    boolean isObjectMessage(Object instance);

    Object getDestination(Object messageProducer);

    String getDestinationName(Object destination);

    String getDestinationType(Object destination);

    String getText(Object textMessage);

    int getMapMessageSize(Object mapMessage);

    Object getObject(Object objectMessage);

    int getSize(Object message);

    void setMessageSize(Object message, int size);

    void setProducerSize(Object jmsProducer, int size);

    void updateMessageSize(Object message, int size);

    void setProducerTag(Object jmsProducer, String tag);

    void setMessageTag(Object message, String tag);
}
