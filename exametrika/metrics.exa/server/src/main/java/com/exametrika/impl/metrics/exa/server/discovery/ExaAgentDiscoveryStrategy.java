/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.discovery;

import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.metrics.exa.server.config.model.ExaAgentDiscoveryStrategySchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.component.discovery.BaseComponentDiscoveryStrategy;
import com.exametrika.impl.component.nodes.AgentComponentNode;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.metrics.exa.server.nodes.ExaAgentComponentNode;
import com.exametrika.impl.metrics.exa.server.nodes.ExaAgentComponentVersionNode;
import com.exametrika.impl.metrics.exa.server.nodes.ExaServerComponentNode;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ExaAgentDiscoveryStrategy} is a node component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaAgentDiscoveryStrategy extends BaseComponentDiscoveryStrategy {
    public ExaAgentDiscoveryStrategy(ExaAgentDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        super(configuration, context);
    }

    @Override
    protected boolean areReferencesResolved(ComponentNode component) {
        ExaAgentComponentVersionNode version = (ExaAgentComponentVersionNode) component.getCurrentVersion();
        return version.getParent() != null && version.getServer() != null;
    }

    @Override
    protected void resolveReferences(List<ComponentNode> components) {
        IPeriodNameManager nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
        IObjectSpace space = spaceSchema.getSpace();

        for (ComponentNode component : components) {
            ExaAgentComponentVersionNode version = (ExaAgentComponentVersionNode) component.getCurrentVersion();

            if (version.getParent() == null) {
                JsonObject properties = component.getCurrentVersion().getProperties();
                if (properties != null) {
                    String nodeName = properties.get("node", null);
                    AgentComponentNode node = findNodeByName(nameManager, space, component, nodeName);

                    if (node != null)
                        ((ExaAgentComponentNode) component).setParent(node);
                }
            }

            if (version.getServer() == null) {
                ExaServerComponentNode server = (ExaServerComponentNode) Selectors.selectServer(((ComponentRootNode) space.getRootNode()).getRootGroup());
                if (server != null)
                    ((ExaAgentComponentNode) component).setServer(server);
            }
        }
    }

    private AgentComponentNode findNodeByName(IPeriodNameManager nameManager, IObjectSpace space, ComponentNode component, String nodeName) {
        if (nodeName == null)
            return null;

        IScopeName scope = Names.getScope(nodeName);
        IPeriodName name = nameManager.findByName(scope);
        if (name == null)
            return null;

        INodeIndex<Long, AgentComponentNode> index = space.getIndex(component.getSchema().getIndexField());
        return index.find(name.getId());
    }
}
