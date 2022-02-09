/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.bridge;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

import com.exametrika.spi.metrics.jvm.boot.IJmsConsumerBridge;


/**
 * The {@link JmsConsumerBridge} represents an implementation of {@link IJmsConsumerBridge}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JmsConsumerBridge implements IJmsConsumerBridge {
    @Override
    public boolean supports(Object request) {
        return request instanceof Message;
    }

    @Override
    public String getDestinationName(Object message) {
        try {
            Message jmsMessage = (Message) message;
            Destination destination = jmsMessage.getJMSDestination();
            if (destination instanceof Queue)
                return ((Queue) destination).getQueueName();
            else
                return ((Topic) destination).getTopicName();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDestinationType(Object message) {
        try {
            Message jmsMessage = (Message) message;
            Destination destination = jmsMessage.getJMSDestination();
            if (destination instanceof Queue)
                return "queue";
            else
                return "topic";
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTag(Object message) {
        try {
            Message jmsMessage = (Message) message;
            return jmsMessage.propertyExists("_exaTraceTag") ? jmsMessage.getStringProperty("_exaTraceTag") : null;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSize(Object message) {
        try {
            Message jmsMessage = (Message) message;
            return jmsMessage.propertyExists("_exaSize") ? jmsMessage.getIntProperty("_exaSize") : 0;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getProperty(String name, Object message) {
        try {
            Message jmsMessage = (Message) message;
            return jmsMessage.getObjectProperty(name);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
