/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.INodeComponentVersion;
import com.exametrika.api.component.nodes.ITransactionComponent;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;


/**
 * The {@link NodeComponentVersionNode} is a node component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NodeComponentVersionNode extends AgentComponentVersionNode implements INodeComponentVersion {
    private static final int HOST_FIELD = 12;
    private static final int TRANSACTIONS_FIELD = 13;

    public NodeComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public IHostComponent getHost() {
        ISingleReferenceField<HostComponentNode> field = getField(HOST_FIELD);
        HostComponentNode host = field.get();
        Assert.checkState(host == null || host.isAccessAlowed());

        return host;
    }

    public void setHost(HostComponentNode host) {
        ISingleReferenceField<HostComponentNode> field = getField(HOST_FIELD);
        field.set(host);
    }

    @Override
    public Iterable<ITransactionComponent> getTransactions() {
        IReferenceField<ITransactionComponent> field = getField(TRANSACTIONS_FIELD);
        return new ComponentIterable<ITransactionComponent>(field);
    }

    public void addTransaction(TransactionComponentNode transaction) {
        IReferenceField<TransactionComponentNode> field = getField(TRANSACTIONS_FIELD);
        field.add(transaction, transaction.getCurrentVersion().getDeletionCount());

        transaction.setPrimaryNode((NodeComponentNode) getComponent());
    }

    public void removeTransaction(TransactionComponentNode transaction) {
        IReferenceField<TransactionComponentNode> field = getField(TRANSACTIONS_FIELD);
        field.remove(transaction);

        transaction.setPrimaryNode(null);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonTransactions = false;
        for (ITransactionComponent node : getTransactions()) {
            if (!jsonTransactions) {
                json.key("transactions");
                json.startArray();
                jsonTransactions = true;
            }

            json.value(getRefId(node));
        }

        if (jsonTransactions)
            json.endArray();
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        ISingleReferenceField<IHostComponent> nodeHostField = node.getField(HOST_FIELD);
        nodeHostField.set(getHost());

        IReferenceField<ITransactionComponent> nodeTransactionsField = node.getField(TRANSACTIONS_FIELD);
        for (ITransactionComponent component : getTransactions())
            nodeTransactionsField.add(component, ((ComponentVersionNode) component.getCurrentVersion()).getDeletionCount());
    }

    @Override
    protected void doComponentCreated() {
        ComponentRootNode root = getSpace().getRootNode();
        root.addNode((INodeComponent) getComponent());
    }

    @Override
    protected void doComponentDeleted() {
        ComponentRootNode root = getSpace().getRootNode();
        root.removeNode((INodeComponent) getComponent());
    }
}