/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.IHostComponentVersion;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;


/**
 * The {@link HostComponentVersionNode} is a host component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HostComponentVersionNode extends AgentComponentVersionNode implements IHostComponentVersion {
    private static final int NODES_FIELD = 12;

    public HostComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public Iterable<INodeComponent> getNodes() {
        IReferenceField<INodeComponent> field = getField(NODES_FIELD);
        return new ComponentIterable<INodeComponent>(field);
    }

    public void addNode(NodeComponentNode node) {
        IReferenceField<NodeComponentNode> field = getField(NODES_FIELD);
        field.add(node, node.getCurrentVersion().getDeletionCount());

        node.setHost((HostComponentNode) getComponent());
    }

    public void removeNode(NodeComponentNode node) {
        IReferenceField<NodeComponentNode> field = getField(NODES_FIELD);
        field.remove(node);

        node.setHost(null);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonNodes = false;
        for (INodeComponent node : getNodes()) {
            if (!jsonNodes) {
                json.key("nodes");
                json.startArray();
                jsonNodes = true;
            }

            json.value(getRefId(node));
        }

        if (jsonNodes)
            json.endArray();
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        IReferenceField<INodeComponent> nodeNodesField = node.getField(NODES_FIELD);
        for (INodeComponent component : getNodes())
            nodeNodesField.add(component, ((ComponentVersionNode) component.getCurrentVersion()).getDeletionCount());
    }

    @Override
    protected void doComponentCreated() {
        ComponentRootNode root = getSpace().getRootNode();
        root.addHost((IHostComponent) getComponent());
    }

    @Override
    protected void doComponentDeleted() {
        ComponentRootNode root = getSpace().getRootNode();
        root.removeHost((IHostComponent) getComponent());
    }
}