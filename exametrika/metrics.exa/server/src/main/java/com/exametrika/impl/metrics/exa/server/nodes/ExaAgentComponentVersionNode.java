/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.nodes;

import com.exametrika.api.component.nodes.IAgentComponent;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.metrics.exa.server.nodes.IExaAgentComponentVersion;
import com.exametrika.api.metrics.exa.server.nodes.IExaServerComponent;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.AgentComponentNode;
import com.exametrika.impl.component.nodes.ComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;


/**
 * The {@link ExaAgentComponentVersionNode} is a exa agent component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaAgentComponentVersionNode extends HealthComponentVersionNode implements IExaAgentComponentVersion {
    private static final int PARENT_FIELD = 11;
    private static final int SERVER_FIELD = 12;

    public ExaAgentComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public IAgentComponent getParent() {
        ISingleReferenceField<AgentComponentNode> field = getField(PARENT_FIELD);
        AgentComponentNode node = field.get();
        Assert.checkState(node == null || node.isAccessAlowed());

        return node;
    }

    public void setParent(AgentComponentNode node) {
        ISingleReferenceField<AgentComponentNode> field = getField(PARENT_FIELD);
        field.set(node);
    }

    @Override
    public IExaServerComponent getServer() {
        ISingleReferenceField<ExaServerComponentNode> field = getField(SERVER_FIELD);
        ExaServerComponentNode server = field.get();
        Assert.checkState(server == null || server.isAccessAlowed());

        return server;
    }

    public void setServer(ExaServerComponentNode server) {
        ISingleReferenceField<ExaServerComponentNode> field = getField(SERVER_FIELD);
        field.set(server);
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        ISingleReferenceField<IAgentComponent> nodeParentField = node.getField(PARENT_FIELD);
        nodeParentField.set(getParent());

        ISingleReferenceField<IExaServerComponent> nodeServerField = node.getField(SERVER_FIELD);
        nodeServerField.set(getServer());
    }
}