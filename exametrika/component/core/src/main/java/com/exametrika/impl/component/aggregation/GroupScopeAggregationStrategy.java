/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.nodes.AggregationNode;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupScopeAggregationStrategy} is a group-based scope aggregation strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupScopeAggregationStrategy implements IScopeAggregationStrategy {
    private final GroupScopeAggregationStrategySchemaConfiguration configuration;
    private final IDatabaseContext context;
    private IObjectSpaceSchema spaceSchema;
    private IPeriodNameManager nameManager;

    public GroupScopeAggregationStrategy(GroupScopeAggregationStrategySchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public ScopeHierarchy getAggregationHierarchy(IAggregationNode node) {
        IScopeName scope = node.getScope();
        IScopeName componentScope = scope;
        if (node instanceof INameNode) {
            IComponentValue value = node.getAggregationField().getValue(false);
            if (value.getMetadata() != null) {
                String mainComponentType = value.getMetadata().get("main", null);
                if (mainComponentType != null) {
                    IPeriod period = node.getPeriod();
                    IAggregationNodeSchema schema = period.getSpace().getSchema().findAggregationNode(mainComponentType);
                    if (schema != null) {
                        INodeIndex<Location, AggregationNode> index = period.getIndex(schema.getPrimaryField());
                        AggregationNode mainNode = index.find(new Location(node.getLocation().getScopeId(), 0));
                        if (mainNode != null)
                            return getAggregationHierarchy(mainNode);
                    }
                }
            }

            if (configuration.hasSubScope())
                componentScope = Names.getScope(scope.getSegments().subList(0, scope.getSegments().size() - 1));
        } else if (node instanceof IStackNode) {
            IStackNode stackNode = (IStackNode) node;
            IEntryPointNode transactionRoot = stackNode.getTransactionRoot();
            if (transactionRoot != null)
                componentScope = transactionRoot.getScope();
        }

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
                        scopes.add(0, group.getScope());
                        group = ((IGroupComponentVersion) group.getCurrentVersion()).getParent();
                    }

                    if (!scopes.isEmpty())
                        break;
                }
            }
        }

        if (!componentScope.equals(scope))
            scopes.add(componentScope);

        scopes.add(scope);

        return new ScopeHierarchy(scopes);
    }

    @Override
    public boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node) {
        return !transactionAggregation;
    }
}
