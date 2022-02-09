/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.INodeComponentVersion;
import com.exametrika.api.component.nodes.ITransactionComponent;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Assert;


/**
 * The {@link NodeComponentNode} is a node component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NodeComponentNode extends AgentComponentNode implements INodeComponent {
    public NodeComponentNode(INode node) {
        super(node);
    }

    public void setHost(HostComponentNode host) {
        NodeComponentVersionNode node = (NodeComponentVersionNode) addVersion();
        node.setHost(host);
    }

    public void addTransaction(TransactionComponentNode transaction) {
        Assert.isTrue(!transaction.getCurrentVersion().isDeleted());

        NodeComponentVersionNode node = (NodeComponentVersionNode) addVersion();
        node.addTransaction(transaction);
    }

    public void removeTransaction(TransactionComponentNode transaction) {
        Assert.isTrue(!transaction.getCurrentVersion().isDeleted());

        NodeComponentVersionNode node = (NodeComponentVersionNode) addVersion();
        node.removeTransaction(transaction);
    }

    @Override
    public void addToIncidentGroups(IIncident incident) {
        super.addToIncidentGroups(incident);

        HostComponentNode host = (HostComponentNode) ((INodeComponentVersion) getCurrentVersion()).getHost();
        if (host != null)
            host.addToIncidentGroups(incident);
    }

    @Override
    protected void doSetUnavailableState() {
        NodeComponentVersionNode currentVersion = (NodeComponentVersionNode) getCurrentVersion();
        for (ITransactionComponent node : currentVersion.getTransactions())
            ((TransactionComponentNode) node).setUnavailableState();

        super.doSetUnavailableState();
    }

    @Override
    protected void doSetNormalState() {
        NodeComponentVersionNode currentVersion = (NodeComponentVersionNode) getCurrentVersion();
        for (ITransactionComponent node : currentVersion.getTransactions())
            ((TransactionComponentNode) node).setNormalState();

        super.doSetNormalState();
    }

    @Override
    protected void doEnableMaintenanceMode(String message) {
        NodeComponentVersionNode currentVersion = (NodeComponentVersionNode) getCurrentVersion();
        for (ITransactionComponent node : currentVersion.getTransactions())
            node.enableMaintenanceMode(message);

        super.doEnableMaintenanceMode(message);
    }

    @Override
    protected void doDisableMaintenanceMode() {
        NodeComponentVersionNode currentVersion = (NodeComponentVersionNode) getCurrentVersion();
        for (ITransactionComponent node : currentVersion.getTransactions())
            node.disableMaintenanceMode();

        super.doDisableMaintenanceMode();
    }

    @Override
    protected void doBeforeDelete() {
        NodeComponentVersionNode currentVersion = (NodeComponentVersionNode) getCurrentVersion();
        for (ITransactionComponent node : currentVersion.getTransactions())
            node.delete();

        super.doBeforeDelete();
    }
}