/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.config.model.NodeDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.HostComponentNode;
import com.exametrika.impl.component.nodes.NodeComponentNode;
import com.exametrika.impl.component.nodes.NodeComponentVersionNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeDiscoveryStrategy} is a node component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class NodeDiscoveryStrategy extends BaseComponentDiscoveryStrategy {
    public NodeDiscoveryStrategy(NodeDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(configuration, context);
    }

    @Override
    protected boolean areReferencesResolved(ComponentNode component) {
        return ((NodeComponentVersionNode) ((NodeComponentNode) component).getCurrentVersion()).getHost() != null;
    }

    @Override
    protected void resolveReferences(List<ComponentNode> components) {
        IPeriodNameManager nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
        IObjectSpace space = spaceSchema.getSpace();

        for (ComponentNode component : components) {
            JsonObject properties = component.getCurrentVersion().getProperties();
            if (properties == null)
                continue;

            String hostName = properties.get("host", null);
            HostComponentNode host = findHostByName(nameManager, space, component, hostName);
            if (host == null) {
                JsonObject nodeProperties = properties.get("nodeProperties", null);
                if (nodeProperties != null)
                    host = findHostByName(nameManager, space, component, (String) nodeProperties.get("host", null));
            }

            if (host == null)
                host = findHostByHostName(space, hostName);

            if (host == null)
                continue;

            host.addNode((NodeComponentNode) component);
        }
    }

    private HostComponentNode findHostByName(IPeriodNameManager nameManager, IObjectSpace space, ComponentNode component, String hostName) {
        if (hostName == null)
            return null;

        IScopeName scope = Names.getScope(hostName);
        IPeriodName name = nameManager.findByName(scope);
        if (name == null)
            return null;

        INodeIndex<Long, HostComponentNode> index = space.getIndex(component.getSchema().getIndexField());
        return index.find(name.getId());
    }

    private HostComponentNode findHostByHostName(IObjectSpace space, String hostName) {
        if (hostName == null)
            return null;

        ComponentRootNode root = space.getRootNode();
        for (IHealthComponent healthComponent : root.getHealthComponents()) {
            if (healthComponent instanceof HostComponentNode) {
                JsonObject properties = healthComponent.getCurrentVersion().getProperties();
                if (properties == null)
                    continue;
                String hostHostName = (String) properties.get("host", null);
                if (hostName.equals(hostHostName))
                    return (HostComponentNode) healthComponent;
            }
        }

        return null;
    }
}
