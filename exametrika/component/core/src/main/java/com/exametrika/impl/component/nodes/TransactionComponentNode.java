/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.ITransactionComponent;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.INumericField;


/**
 * The {@link TransactionComponentNode} is a transaction component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TransactionComponentNode extends HealthComponentNode implements ITransactionComponent {
    private static final int RETENTION_COUNTER_FIELD = 9;

    public TransactionComponentNode(INode node) {
        super(node);
    }

    public int incrementRetentionCounter() {
        INumericField field = getField(RETENTION_COUNTER_FIELD);
        int counter = field.getInt() + 1;
        field.setInt(counter);
        return counter;
    }

    public void resetRetentionCounter() {
        INumericField field = getField(RETENTION_COUNTER_FIELD);
        field.setInt(0);
    }

    public void setPrimaryNode(NodeComponentNode node) {
        TransactionComponentVersionNode transaction = (TransactionComponentVersionNode) addVersion();
        transaction.setPrimaryNode(node);
    }

    @Override
    public void addToIncidentGroups(IIncident incident) {
        super.addToIncidentGroups(incident);

        NodeComponentNode node = (NodeComponentNode) ((TransactionComponentVersionNode) getCurrentVersion()).getPrimaryNode();
        if (node != null)
            node.addToIncidentGroups(incident);
    }

    @Override
    public boolean allowExecution() {
        if (!super.allowExecution())
            return false;

        NodeComponentNode node = (NodeComponentNode) ((TransactionComponentVersionNode) getCurrentVersion()).getPrimaryNode();
        if (node != null)
            return node.allowExecution();
        else
            return true;
    }

    @Override
    protected boolean supportsAvailability() {
        return true;
    }
}