/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.component.config.model.BaseComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.component.nodes.HealthComponentNode;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link BaseComponentDiscoveryStrategy} is a base component discovery strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class BaseComponentDiscoveryStrategy implements IComponentDiscoveryStrategy {
    protected final BaseComponentDiscoveryStrategySchemaConfiguration configuration;
    protected final IDatabaseContext context;
    protected IObjectSpaceSchema spaceSchema;

    public BaseComponentDiscoveryStrategy(BaseComponentDiscoveryStrategySchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void processDiscovered(List<Pair<Long, JsonObject>> existingComponents) {
        if (spaceSchema == null)
            spaceSchema = context.getSchemaSpace().getCurrentSchema().findSchemaById("space:component.component");

        IObjectSpace space = spaceSchema.getSpace();

        List<ComponentNode> newComponents = new ArrayList<ComponentNode>();
        List<ComponentNode> unresolvedComponents = new ArrayList<ComponentNode>();
        for (Pair<Long, JsonObject> pair : existingComponents) {
            long scopeId = pair.getKey();
            JsonObject metadata = pair.getValue();

            String componentSchemaName = getComponentSchema(metadata);
            if (componentSchemaName == null)
                continue;

            ComponentNodeSchema componentSchema = spaceSchema.findNode(componentSchemaName);

            INodeIndex<Long, ComponentNode> index = space.getIndex(componentSchema.getIndexField());
            ComponentNode component = index.find(scopeId);
            if (component == null || component.getCurrentVersion().isDeleted() || !component.getCurrentVersion().getGroups().iterator().hasNext()) {
                if (component == null) {
                    component = space.createNode(scopeId, componentSchema);
                    if (metadata != null) {
                        List<String> tags = metadata.select("nodeProperties?.tags?", null);
                        if (tags != null)
                            component.setTags(tags);
                    }
                }

                newComponents.add(component);
            }

            if (component instanceof HealthComponentNode && isDynamic(metadata))
                ((HealthComponentNode) component).setDynamic();

            setProperties(component, metadata);

            if (!areReferencesResolved(component))
                unresolvedComponents.add(component);
        }

        if (!unresolvedComponents.isEmpty())
            resolveReferences(unresolvedComponents);

        for (ComponentNode component : newComponents) {
            ComponentNodeSchema schema = component.getSchema();
            for (IGroupDiscoveryStrategy strategy : schema.getGroupDiscoveryStrategies()) {
                List<IGroupComponent> groups = strategy.getGroups(component, component, 0);
                for (IGroupComponent group : groups)
                    group.addComponent(component);
            }
        }
    }

    protected void setProperties(ComponentNode component, JsonObject metadata) {
        component.setProperties(metadata);
    }

    protected boolean isDynamic(JsonObject metadata) {
        if (metadata != null)
            return metadata.select("nodeProperties?.dynamic?", false);
        else
            return false;
    }

    protected String getComponentSchema(JsonObject metadata) {
        return configuration.getComponent();
    }

    protected boolean areReferencesResolved(ComponentNode component) {
        return true;
    }

    protected void resolveReferences(List<ComponentNode> components) {
    }
}
