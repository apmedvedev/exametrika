/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.nodes;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.metrics.exa.server.nodes.IExaAgentComponent;
import com.exametrika.impl.component.nodes.AgentComponentNode;
import com.exametrika.impl.component.nodes.HealthComponentNode;


/**
 * The {@link ExaAgentComponentNode} is a exa agent component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaAgentComponentNode extends HealthComponentNode implements IExaAgentComponent {
    public ExaAgentComponentNode(INode node) {
        super(node);
    }

    public void setParent(AgentComponentNode node) {
        ExaAgentComponentVersionNode version = (ExaAgentComponentVersionNode) addVersion();
        version.setParent(node);

        node.addSubComponent(this);
    }

    public void setServer(ExaServerComponentNode server) {
        ExaAgentComponentVersionNode version = (ExaAgentComponentVersionNode) addVersion();
        version.setServer(server);
    }
}