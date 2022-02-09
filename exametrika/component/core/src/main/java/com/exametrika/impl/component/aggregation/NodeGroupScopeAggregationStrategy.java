/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.NodeGroupScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeGroupScopeAggregationStrategy} is a node group-based scope aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class NodeGroupScopeAggregationStrategy implements IScopeAggregationStrategy {
    private final NodeGroupScopeAggregationStrategySchemaConfiguration configuration;
    private final IDatabaseContext context;
    private IObjectSpaceSchema spaceSchema;
    private IPeriodNameManager nameManager;

    public NodeGroupScopeAggregationStrategy(NodeGroupScopeAggregationStrategySchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public ScopeHierarchy getAggregationHierarchy(IAggregationNode node) {
        Assert.isTrue(node instanceof IStackNode);
        IStackNode stackNode = (IStackNode) node;
        IScopeName scope = node.getScope();

        IScopeName componentScope = stackNode.getRoot().getScope();
        componentScope = Names.getScope(scope.getSegments().subList(0, scope.getSegments().size() - 1));

        if (spaceSchema == null) {
            spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");
            nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
        }

        List<IScopeName> scopes = new ArrayList<IScopeName>();
        IPeriodName name = nameManager.findByName(componentScope);
        if (name != null) {
            IObjectSpace space = spaceSchema.getSpace();
            INodeIndex<Long, IComponent> index = space.findIndex("componentIndex");
            IComponent component = index.find(name.getId());
            if (component != null) {
                List<IGroupComponent> groups = new ArrayList<IGroupComponent>();
                Iterator<IGroupComponent> it = component.getCurrentVersion().getGroups().iterator();
                while (it.hasNext()) {
                    IGroupComponent group = it.next();
                    if (((GroupComponentSchemaConfiguration) group.getConfiguration()).isAggregationGroup())
                        groups.add(group);
                }

                for (IGroupComponent group : groups) {
                    if (groups.size() > 1) {
                        JsonObject properties = group.getCurrentVersion().getProperties();
                        if (properties == null)
                            continue;

                        if (!configuration.getHierarchyType().equals(properties.get("hierarchyType", null)))
                            continue;
                    }

                    while (group != null && ((GroupComponentSchemaConfiguration) group.getConfiguration()).isAggregationGroup()) {
                        scopes.add(0, Names.getScope("nodes." + group.getScope().toString()));
                        group = ((IGroupComponentVersion) group.getCurrentVersion()).getParent();
                    }

                    if (!scopes.isEmpty())
                        break;
                }
            }
        }

        scopes.add(Names.getScope("nodes." + componentScope.toString()));
        scopes.add(Names.getScope("nodes." + scope.toString()));

        return new ScopeHierarchy(scopes);
    }

    @Override
    public boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node) {
        return true;
    }
}
