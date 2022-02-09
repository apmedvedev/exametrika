/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.client;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import com.exametrika.api.instrument.client.IConnection;


/**
 * The {@link JdkConnection} is connection to JDK instrumentation agent.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdkConnection implements IConnection {
    private static final String AGENT_MBEAN_NAME = "com.exametrika.agent:type=Agent";
    private final JMXConnector connector;

    public JdkConnection(JMXConnector connector) {
        if (connector == null)
            throw new IllegalArgumentException();

        this.connector = connector;
    }

    @Override
    public boolean isStarted() {
        try {
            return (Boolean) connector.getMBeanServerConnection().getAttribute(ObjectName.getInstance(AGENT_MBEAN_NAME), "Started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(String configurationPath) {
        try {
            connector.getMBeanServerConnection().invoke(ObjectName.getInstance(AGENT_MBEAN_NAME), "start", new Object[]{configurationPath}, new String[]{String.class.getName()});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            connector.getMBeanServerConnection().invoke(ObjectName.getInstance(AGENT_MBEAN_NAME), "stop", new Object[0], new String[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
