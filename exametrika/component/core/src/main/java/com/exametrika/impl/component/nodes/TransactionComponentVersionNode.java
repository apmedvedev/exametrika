/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.ITransactionComponent;
import com.exametrika.api.component.nodes.ITransactionComponentVersion;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TransactionComponentVersionNode} is a transaction component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TransactionComponentVersionNode extends HealthComponentVersionNode implements ITransactionComponentVersion {
    private static final int PRIMARY_NODE_FIELD = 11;

    public TransactionComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public INodeComponent getPrimaryNode() {
        ISingleReferenceField<NodeComponentNode> field = getField(PRIMARY_NODE_FIELD);
        NodeComponentNode node = field.get();
        Assert.checkState(node == null || node.isAccessAlowed());

        return node;
    }

    public void setPrimaryNode(NodeComponentNode node) {
        ISingleReferenceField<NodeComponentNode> field = getField(PRIMARY_NODE_FIELD);
        field.set(node);
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        ISingleReferenceField<INodeComponent> transactionNodeField = node.getField(PRIMARY_NODE_FIELD);
        transactionNodeField.set(getPrimaryNode());
    }

    @Override
    protected boolean supportsAvailability() {
        return true;
    }

    @Override
    protected boolean isComponentAvailable() {
        INodeComponent node = getPrimaryNode();
        if (node != null) {
            NodeComponentVersionNode nodeVersion = (NodeComponentVersionNode) node.getCurrentVersion();
            return nodeVersion.isComponentAvailable();
        } else
            return true;
    }

    @Override
    protected void doComponentCreated() {
        ComponentRootNode root = getSpace().getRootNode();
        root.addTransaction((ITransactionComponent) getComponent());
    }

    @Override
    protected void doComponentDeleted() {
        ComponentRootNode root = getSpace().getRootNode();
        root.removeTransaction((ITransactionComponent) getComponent());
    }
}