/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IAgentComponent;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AgentComponentNode} is an agent component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AgentComponentNode extends HealthComponentNode implements IAgentComponent {
    public AgentComponentNode(INode node) {
        super(node);
    }

    public void addSubComponent(ComponentNode component) {
        Assert.isTrue(!component.getCurrentVersion().isDeleted());

        AgentComponentVersionNode node = (AgentComponentVersionNode) addVersion();
        node.addSubComponent(component);
    }

    public void removeSubComponent(ComponentNode component) {
        component.delete();

        AgentComponentVersionNode node = (AgentComponentVersionNode) addVersion();
        node.removeSubComponent(component);
    }

    @Override
    protected boolean supportsAvailability() {
        return true;
    }

    @Override
    protected void doSetUnavailableState() {
        AgentComponentVersionNode currentVersion = (AgentComponentVersionNode) getCurrentVersion();
        for (IComponent node : currentVersion.getSubComponents()) {
            if (node instanceof HealthComponentNode)
                ((HealthComponentNode) node).setUnavailableState();
        }
    }

    @Override
    protected void doSetNormalState() {
        AgentComponentVersionNode currentVersion = (AgentComponentVersionNode) getCurrentVersion();
        for (IComponent node : currentVersion.getSubComponents()) {
            if (node instanceof HealthComponentNode)
                ((HealthComponentNode) node).setNormalState();
        }
    }

    @Override
    protected void doEnableMaintenanceMode(String message) {
        AgentComponentVersionNode currentVersion = (AgentComponentVersionNode) getCurrentVersion();
        for (IComponent node : currentVersion.getSubComponents()) {
            if (node instanceof IHealthComponent)
                ((IHealthComponent) node).enableMaintenanceMode(message);
        }
    }

    @Override
    protected void doDisableMaintenanceMode() {
        AgentComponentVersionNode currentVersion = (AgentComponentVersionNode) getCurrentVersion();
        for (IComponent node : currentVersion.getSubComponents()) {
            if (node instanceof IHealthComponent)
                ((IHealthComponent) node).disableMaintenanceMode();
        }
    }

    @Override
    protected void doBeforeDelete() {
        AgentComponentVersionNode currentVersion = (AgentComponentVersionNode) getCurrentVersion();
        for (IComponent node : currentVersion.getSubComponents())
            node.delete();

        super.doBeforeDelete();
    }
}