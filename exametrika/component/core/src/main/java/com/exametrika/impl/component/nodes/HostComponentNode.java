/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Assert;


/**
 * The {@link HostComponentNode} is a host component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HostComponentNode extends AgentComponentNode implements IHostComponent {
    public HostComponentNode(INode node) {
        super(node);
    }

    public void addNode(NodeComponentNode node) {
        Assert.isTrue(!node.getCurrentVersion().isDeleted());

        HostComponentVersionNode host = (HostComponentVersionNode) addVersion();
        host.addNode(node);
    }

    public void removeNode(NodeComponentNode node) {
        node.delete();

        HostComponentVersionNode host = (HostComponentVersionNode) addVersion();
        host.removeNode(node);
    }

    @Override
    protected void doEnableMaintenanceMode(String message) {
        HostComponentVersionNode currentVersion = (HostComponentVersionNode) getCurrentVersion();
        for (INodeComponent node : currentVersion.getNodes())
            node.enableMaintenanceMode(message);

        super.doEnableMaintenanceMode(message);
    }

    @Override
    protected void doDisableMaintenanceMode() {
        HostComponentVersionNode currentVersion = (HostComponentVersionNode) getCurrentVersion();
        for (INodeComponent node : currentVersion.getNodes())
            node.disableMaintenanceMode();

        super.doDisableMaintenanceMode();
    }

    @Override
    protected void doBeforeDelete() {
        HostComponentVersionNode currentVersion = (HostComponentVersionNode) getCurrentVersion();
        for (INodeComponent node : currentVersion.getNodes())
            node.delete();

        super.doBeforeDelete();
    }
}