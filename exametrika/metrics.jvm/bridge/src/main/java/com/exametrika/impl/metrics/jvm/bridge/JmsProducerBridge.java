/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.bridge;

import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import com.exametrika.spi.metrics.jvm.boot.IJmsProducerBridge;


/**
 * The {@link JmsProducerBridge} represents an implementation of {@link IJmsProducerBridge}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JmsProducerBridge implements IJmsProducerBridge {
    private static final boolean supportsJmsProducer;

    static {
        boolean value = false;
        try {
            value = JMSProducer.class != null;
        } catch (Throwable e) {
        }

        supportsJmsProducer = value;
    }

    @Override
    public boolean supports(Object request) {
        return request instanceof Message || request instanceof MessageProducer || isJmsProducer(request);
    }

    @Override
    public boolean isJmsProducer(Object instance) {
        return supportsJmsProducer && isInstanceOfJmsProducer(instance);
    }

    @Override
    public boolean isMessage(Object instance) {
        return instance instanceof Message;
    }

    @Override
    public boolean isBytesOrStreamMessage(Object instance) {
        return instance instanceof BytesMessage || instance instanceof StreamMessage;
    }

    @Override
    public boolean isTextMessage(Object instance) {
        return instance instanceof TextMessage;
    }

    @Override
    public boolean isMapMessage(Object instance) {
        return instance instanceof MapMessage;
    }

    @Override
    public boolean isObjectMessage(Object instance) {
        return instance instanceof ObjectMessage;
    }

    @Override
    public Object getDestination(Object messageProducer) {
        try {
            MessageProducer jmsMessageProducer = (MessageProducer) messageProducer;
            return jmsMessageProducer.getDestination();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDestinationName(Object destination) {
        try {
            if (destination instanceof Queue)
                return ((Queue) destination).getQueueName();
            else
                return ((Topic) destination).getTopicName();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDestinationType(Object destination) {
        if (destination instanceof Queue)
            return "queue";
        else
            return "topic";
    }

    @Override
    public String getText(Object textMessage) {
        try {
            TextMessage jmsTextMessage = (TextMessage) textMessage;
            return jmsTextMessage.getText();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getMapMessageSize(Object mapMessage) {
        try {
            MapMessage jmsMapMessage = (MapMessage) mapMessage;
            int size = 0;
            for (Enumeration e = jmsMapMessage.getMapNames(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                Object value = jmsMapMessage.getObject(name);
                size += 2 * name.length() + getPrimitiveSize(value);
            }
            return size;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getObject(Object objectMessage) {
        try {
            ObjectMessage jmsObjectMessage = (ObjectMessage) objectMessage;
            return jmsObjectMessage.getObject();
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
    public void setMessageSize(Object message, int size) {
        try {
            Message jmsMessage = (Message) message;
            jmsMessage.setIntProperty("_exaSize", size);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProducerSize(Object jmsProducer, int size) {
        JMSProducer producer = (JMSProducer) jmsProducer;
        producer.setProperty("_exaSize", size);
    }

    @Override
    public void updateMessageSize(Object message, int size) {
        try {
            Message jmsMessage = (Message) message;
            if (jmsMessage.propertyExists("_exaSize"))
                size += jmsMessage.getIntProperty("_exaSize");
            jmsMessage.setIntProperty("_exaSize", size);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProducerTag(Object jmsProducer, String tag) {
        JMSProducer producer = (JMSProducer) jmsProducer;
        producer.setProperty("_exaTraceTag", tag);
    }

    @Override
    public void setMessageTag(Object message, String tag) {
        try {
            Message jmsMessage = (Message) message;
            jmsMessage.setStringProperty("_exaTraceTag", tag);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInstanceOfJmsProducer(Object request) {
        return request instanceof JMSProducer;
    }

    private int getPrimitiveSize(Object value) {
        if (value instanceof byte[])
            return ((byte[]) value).length;
        else if (value instanceof String)
            return 2 * ((String) value).length();
        else if (value instanceof Boolean || value instanceof Byte)
            return 1;
        else if (value instanceof Short || value instanceof Character)
            return 2;
        else if (value instanceof Integer || value instanceof Float)
            return 4;
        else if (value instanceof Long || value instanceof Double)
            return 8;
        else
            return 0;
    }
}
