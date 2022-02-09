/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.exametrika.api.instrument.client.IConnection;
import com.exametrika.api.instrument.client.IConnectionFactory;
import com.exametrika.api.instrument.client.ProcessInfo;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * The {@link JdkConnectionFactory} is a factory for connection to JDK instrumentation agent.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdkConnectionFactory implements IConnectionFactory {
    private static final String AGENT_PROPERTY_NAME = "com.exametrika.agent";
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    @Override
    public List<ProcessInfo> getAvailableProcesses() {
        List<ProcessInfo> list = new ArrayList<ProcessInfo>();
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String description = descriptor.displayName();
            if (description.contains("com.exametrika.api.instrument.client"))
                continue;

            if (description.isEmpty())
                description = "<unknown>";

            list.add(new ProcessInfo(descriptor.id(), description));
        }

        return list;
    }

    @Override
    public IConnection connect(String pid, String agentPath) {
        return connect(pid, agentPath, true);
    }

    @Override
    public IConnection connect(String pid) {
        return connect(pid, null, false);
    }

    private IConnection connect(String pid, String agentPath, boolean forceLoad) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            String agentLoaded = vm.getSystemProperties().getProperty(AGENT_PROPERTY_NAME);
            if (agentLoaded == null) {
                if (!forceLoad)
                    return null;

                vm.loadAgent(agentPath, agentPath);
            }

            String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

            if (connectorAddress == null) {
                String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
                vm.loadAgent(agent);

                connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            }

            vm.detach();

            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            JMXConnector connector = JMXConnectorFactory.connect(url);

            return new JdkConnection(connector);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
